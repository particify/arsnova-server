/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nimbusds.jwt.SignedJWT
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.UserService
import org.altcha.altcha.v2.Altcha
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

private const val TIMEOUT_SECONDS = 10L
private const val UNAUTHORIZED_CLOSE_CODE = 4401

/** Its resolver requires an account, so a reusable token is denied. */
private const val ACCOUNT_QUERY = "currentUser { id }"

/** The shipped `security.challenge.reusable-token-validity`. */
private const val VALIDITY_SECONDS = 5 * 60L

private const val OTHER_QUERY = """userByDisplayId(displayId: "unknown") { id }"""

private const val PASSWORD_RESET =
    """mutation { requestUserPasswordReset(mailAddress: "unknown@example.com") }"""

/** Solving takes a single attempt, so that every test can afford several challenges. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["security.challenge.iteration-cost=1", "security.challenge.max-iterations=1"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ReusableChallengeTokenHttpTests {
  @LocalServerPort var port: Int = 0
  @Autowired lateinit var jsonMapper: JsonMapper
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var tokens: ReusableChallengeTokens
  @Autowired lateinit var userService: UserService
  @Autowired lateinit var userJwtAuthenticationProvider: UserJwtAuthenticationProvider
  @Autowired lateinit var challengeAuthenticationProvider: ChallengeAuthenticationProvider

  private val client = HttpClient.newHttpClient()

  @Test
  fun shouldIssueReusableTokenForSolution() {
    val token = issueToken(solveChallenge())
    repeat(2) { assertReachesResolver(token) }
  }

  /** The solution is consumed as the token is issued, for either kind of token. */
  @Test
  fun shouldIssueOneTokenPerSolution() {
    val solution = solveChallenge()
    issueToken(solution)
    Assertions.assertEquals(
        HttpStatus.BAD_REQUEST.value(), postChallenge(solution, reusable = true).statusCode())
    val singleUseToken = tokenOf(postChallenge(solution))
    Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED.value(), postGraphQl(singleUseToken, PASSWORD_RESET).statusCode())
  }

  @Test
  fun shouldKeepSingleUseTokenByDefault() {
    val token = tokenOf(postChallenge(solveChallenge()))
    Assertions.assertEquals(HttpStatus.OK.value(), postGraphQl(token, PASSWORD_RESET).statusCode())
    Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED.value(), postGraphQl(token, PASSWORD_RESET).statusCode())
  }

  /** No resolver allows the role, so every operation is denied by its own authorization. */
  @Test
  fun shouldBeDeniedByResolvers() {
    val token = issueToken(solveChallenge())
    listOf(
            "{ $OTHER_QUERY }",
            "{ $ACCOUNT_QUERY $OTHER_QUERY }",
            "{ ...account } fragment account on Query { $ACCOUNT_QUERY }",
            "{ ... on Query { $ACCOUNT_QUERY } }",
            PASSWORD_RESET)
        .forEach { assertForbidden(postGraphQl(token, it), it) }
  }

  /** With an operation which allows the role. */
  @Test
  fun shouldReachOperationAllowingRole() {
    val token = issueToken(solveChallenge())
    val query =
        """{ userPasswordStrength(query: { password: "x" }) { acceptable relativeStrength } }"""
    val response = postGraphQl(token, query)
    Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
    val body = jsonMapper.readTree(response.body())
    Assertions.assertTrue(body.at("/errors").isMissingNode, response.body())
    Assertions.assertTrue(
        body.at("/data/userPasswordStrength/acceptable").isBoolean, response.body())
    Assertions.assertTrue(
        body.at("/data/userPasswordStrength/relativeStrength").isNumber, response.body())
  }

  /** Not `NOT_FOUND` either, which would disclose whether the room exists. */
  @Test
  fun shouldDenyPermissionCheckedOperation() {
    val token = issueToken(solveChallenge())
    val query = """{ roomById(id: "${UUID.randomUUID()}") { id } }"""
    assertForbidden(postGraphQl(token, query), query)
  }

  @Test
  fun shouldGrantOnlyReusableChallengeRole() {
    val token = issueToken(solveChallenge())
    Assertions.assertEquals(
        listOf(CHALLENGE_SOLVED_REUSABLE_ROLE),
        SignedJWT.parse(token).jwtClaimsSet.getStringListClaim("roles"))
    Assertions.assertEquals(
        listOf("ROLE_$CHALLENGE_SOLVED_REUSABLE_ROLE"),
        ReusableChallengeAuthentication(token).authorities.map { it.authority })
  }

  @Test
  fun shouldNotAuthenticateRestRequests() {
    val token = issueToken(solveChallenge())
    val guestAccount =
        send(
            request("/auth/guest-account", token).POST(HttpRequest.BodyPublishers.noBody()).build())
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED.value(), guestAccount.statusCode())
    val internalToken =
        send(
            request("/jwt", token)
                .header("X-Forwarded-Uri", "/api/room/${UUID.randomUUID()}/contents")
                .GET()
                .build())
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), internalToken.statusCode())
  }

  @Test
  fun shouldNotAuthenticateWebSocketSession() {
    val closed = CompletableFuture<Int>()
    val webSocket =
        client
            .newWebSocketBuilder()
            .subprotocols("graphql-transport-ws")
            .buildAsync(URI("ws://localhost:$port/api/graphql/ws"), ClosingListener(closed))
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    val token = issueToken(solveChallenge())
    val init =
        mapOf(
            "type" to "connection_init",
            "payload" to mapOf(HttpHeaders.AUTHORIZATION to "Bearer $token"))
    webSocket.sendText(jsonMapper.writeValueAsString(init), true).get()
    Assertions.assertEquals(UNAUTHORIZED_CLOSE_CODE, closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
  }

  @Test
  fun shouldRejectExpiredTokenWithEmptyUnauthorizedResponse() {
    val issuedAt = Instant.now().minusSeconds(EXPIRED_AGE_SECONDS)
    val response = postGraphQl(tokens.issue(UUID.randomUUID(), issuedAt), "{ $ACCOUNT_QUERY }")
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode())
    Assertions.assertEquals("", response.body())
  }

  @Test
  fun shouldIssueTokenValidForConfiguredValidity() {
    val claims = SignedJWT.parse(issueToken(solveChallenge())).jwtClaimsSet
    Assertions.assertEquals(
        VALIDITY_SECONDS,
        Duration.between(claims.issueTime.toInstant(), claims.expirationTime.toInstant()).seconds)
  }

  @Test
  fun shouldAcceptTokenWithinItsValidity() {
    val issuedAt = Instant.now().minusSeconds(VALID_AGE_SECONDS)
    assertReachesResolver(tokens.issue(UUID.randomUUID(), issuedAt))
  }

  /** Signed with the JWT secret itself, which the token's own key is derived from. */
  @Test
  fun shouldRejectTokenWithClaimsButWithoutDerivedKey() {
    val forged =
        jwtUtils.encodeJwt("reusable-challenge-token", listOf(CHALLENGE_SOLVED_REUSABLE_ROLE))
    Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED.value(), postGraphQl(forged, "{ $ACCOUNT_QUERY }").statusCode())
  }

  @Test
  fun shouldNotAcceptTokenAsUserOrChallengeToken() {
    val token = issueToken(solveChallenge())
    assertThrows<AuthenticationException> {
      userJwtAuthenticationProvider.authenticate(UserJwtAuthentication(token))
    }
    assertThrows<AuthenticationException> {
      challengeAuthenticationProvider.authenticate(UserJwtAuthentication(token))
    }
  }

  @Test
  fun shouldNotAcceptOtherTokensAsReusableToken() {
    val user = userService.createAccount()
    val userToken = jwtUtils.encodeJwt(user.id.toString(), listOf())
    val challengeToken = tokenOf(postChallenge(solveChallenge()))
    listOf(userToken, challengeToken).forEach { assertThrows<Exception> { tokens.verify(it) } }
  }

  /** Each filter only reads the tokens of its own kind, so neither logs a failed verification. */
  @Test
  fun shouldLeaveTokensToTheirOwnFilter() {
    val user = userService.createAccount()
    val userToken = jwtUtils.encodeJwt(user.id.toString(), listOf())
    val reusableToken = issueToken(solveChallenge())
    val events =
        captureDebugLog(
            ReusableChallengeAuthenticationFilter::class.java,
            ChallengeJwtAuthenticationFilter::class.java) {
              postGraphQl(userToken, "{ $ACCOUNT_QUERY }")
              postGraphQl(reusableToken, "{ $ACCOUNT_QUERY }")
            }
    Assertions.assertEquals(listOf<String>(), events.map { "${it.loggerName}: ${it.message}" })
  }

  /** The counterpart to the test above, which would pass as well if nothing were logged at all. */
  @Test
  fun shouldVerifyTokenClaimingToBeReusableToken() {
    val forged =
        jwtUtils.encodeJwt("reusable-challenge-token", listOf(CHALLENGE_SOLVED_REUSABLE_ROLE))
    val events =
        captureDebugLog(ReusableChallengeAuthenticationFilter::class.java) {
          postGraphQl(forged, "{ $ACCOUNT_QUERY }")
        }
    Assertions.assertEquals(
        listOf("Reusable challenge token authentication failed"), events.map { it.message })
  }

  private fun captureDebugLog(vararg sources: Class<*>, block: () -> Unit): List<ILoggingEvent> {
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    val loggers = sources.map { LoggerFactory.getLogger(it) as Logger }
    val levels = loggers.map { it.level }
    loggers.forEach {
      it.level = Level.DEBUG
      it.addAppender(appender)
    }
    try {
      block()
    } finally {
      loggers.zip(levels).forEach { (logger, level) ->
        logger.detachAppender(appender)
        logger.level = level
      }
    }
    return appender.list.toList()
  }

  /**
   * The token authenticated the request, so that the resolver's own authorization denies it:
   * `UNAUTHORIZED` otherwise.
   */
  private fun assertReachesResolver(token: String) {
    assertForbidden(postGraphQl(token, "{ $ACCOUNT_QUERY }"), ACCOUNT_QUERY)
  }

  /** Every selected field is denied, and none of them resolves. */
  private fun assertForbidden(response: HttpResponse<String>, query: String) {
    Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), query)
    val body = jsonMapper.readTree(response.body())
    val errors = body.at("/errors")
    Assertions.assertFalse(errors.isEmpty, response.body())
    errors.forEach {
      Assertions.assertEquals(
          "FORBIDDEN", it.at("/extensions/classification").asString(), response.body())
    }
    val data = body.at("/data")
    Assertions.assertTrue(
        data.isMissingNode || data.isNull || data.all { it.isNull }, response.body())
  }

  private fun solveChallenge(): String {
    val response = send(request("/challenge").GET().build())
    val challenge = jsonMapper.readValue(response.body(), Altcha.Challenge::class.java)
    val solution = Altcha.solveChallenge(challenge, Altcha.kdf(challenge.parameters().algorithm()))
    return Base64.getEncoder()
        .encodeToString(jsonMapper.writeValueAsBytes(Altcha.Payload(challenge, solution)))
  }

  private fun issueToken(solution: String): String =
      tokenOf(postChallenge(solution, reusable = true))

  private fun tokenOf(response: HttpResponse<String>): String {
    Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
    return jsonMapper.readTree(response.body()).at("/token").asString()
  }

  private fun postChallenge(solution: String, reusable: Boolean = false): HttpResponse<String> {
    val body =
        mapOf("solution" to solution) + (if (reusable) mapOf("reusable" to true) else mapOf())
    return send(
        request("/challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)))
            .build())
  }

  private fun postGraphQl(token: String, query: String): HttpResponse<String> =
      send(
          request("/graphql", token)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(mapOf("query" to query))))
              .build())

  private fun request(path: String, token: String? = null): HttpRequest.Builder {
    val builder = HttpRequest.newBuilder(URI("http://localhost:$port/api$path"))
    return token?.let { builder.header(HttpHeaders.AUTHORIZATION, "Bearer $it") } ?: builder
  }

  private fun send(request: HttpRequest): HttpResponse<String> =
      client.send(request, HttpResponse.BodyHandlers.ofString())

  private class ClosingListener(private val closed: CompletableFuture<Int>) : WebSocket.Listener {
    override fun onClose(
        webSocket: WebSocket,
        statusCode: Int,
        reason: String
    ): CompletionStage<*>? {
      closed.complete(statusCode)
      return null
    }
  }

  companion object {
    /** Beyond the token's validity plus the leeway the decoder grants. */
    private const val EXPIRED_AGE_SECONDS = VALIDITY_SECONDS + 2 * 60L

    /** Close to the end of the token's validity. */
    private const val VALID_AGE_SECONDS = VALIDITY_SECONDS - 60L
  }
}
