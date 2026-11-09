/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val ISSUER_PATH = "/realms/test"
private const val KEY_SIZE = 2048
private const val TOKEN_LIFETIME_SECONDS = 300L
private const val STATUS_OK = 200
private const val STATUS_BAD_REQUEST = 400
private const val STATUS_UNAUTHORIZED = 401
private const val STATUS_UNAVAILABLE = 503

/**
 * An OpenID provider serving discovery, JWKS and a token endpoint in process, so a login can be
 * driven through the filter chain. The authorization endpoint is never called: a test reads the
 * authorization request from the redirect and hands the provider the claims to issue for it.
 *
 * The token endpoint checks the client secret and the PKCE verifier, so a login only succeeds when
 * both are sent.
 */
class OidcTestProvider(private val clientId: String, private val clientSecret: String) {
  private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
  private val key: RSAKey = RSAKeyGenerator(KEY_SIZE).keyID(UUID.randomUUID().toString()).generate()
  private val pendingLogins = ConcurrentHashMap<String, PendingLogin>()
  private val discoveryRequests = AtomicInteger()

  /** While false, every endpoint answers 503, as a provider being deployed does. */
  @Volatile var available = true

  /**
   * While false, the discovery document names the issuer only, as a misconfigured provider's does.
   */
  @Volatile var servesValidMetadata = true

  val issuer: String
    get() = "http://localhost:${server.address.port}$ISSUER_PATH"

  val discoveryRequestCount: Int
    get() = discoveryRequests.get()

  init {
    server.createContext("$ISSUER_PATH/.well-known/openid-configuration") {
      discoveryRequests.incrementAndGet()
      respond(it) { if (servesValidMetadata) discovery() else issuerOnlyDiscovery() }
    }
    server.createContext("$ISSUER_PATH/jwks") {
      respond(it) { JWKSet(key.toPublicJWK()).toString() }
    }
    server.createContext("$ISSUER_PATH/token", this::handleToken)
    server.start()
  }

  fun stop() {
    server.stop(0)
  }

  /**
   * Accepts the authorization request [authorizationUrl] points at and returns the code the
   * provider would redirect back with. [claims] go into the ID token as they are, `sub` included.
   */
  fun authorize(authorizationUrl: String, claims: Map<String, Any>): String {
    val parameters = parseQuery(URI.create(authorizationUrl).rawQuery)
    val code = UUID.randomUUID().toString()
    pendingLogins[code] =
        PendingLogin(
            claims,
            parameters["nonce"],
            checkNotNull(parameters["code_challenge"]) { "The request carries no PKCE challenge." })
    return code
  }

  private fun discovery(): String =
      JSONObjectUtils.toJSONString(
          mapOf(
              "issuer" to issuer,
              "authorization_endpoint" to "$issuer/auth",
              "token_endpoint" to "$issuer/token",
              "jwks_uri" to "$issuer/jwks",
              "response_types_supported" to listOf("code"),
              "subject_types_supported" to listOf("public"),
              "id_token_signing_alg_values_supported" to listOf("RS256"),
              "token_endpoint_auth_methods_supported" to listOf("client_secret_basic"),
              "code_challenge_methods_supported" to listOf("S256")))

  private fun issuerOnlyDiscovery(): String =
      JSONObjectUtils.toJSONString(mapOf("issuer" to issuer))

  private fun handleToken(exchange: HttpExchange) {
    if (!available) {
      send(exchange, STATUS_UNAVAILABLE, "")
      return
    }
    val parameters = parseQuery(exchange.requestBody.readAllBytes().decodeToString())
    val login = parameters["code"]?.let { pendingLogins.remove(it) }
    when {
      exchange.requestHeaders.getFirst("Authorization") != basicCredentials() ->
          send(exchange, STATUS_UNAUTHORIZED, """{"error":"invalid_client"}""")
      login == null || challenge(parameters["code_verifier"]) != login.codeChallenge ->
          send(exchange, STATUS_BAD_REQUEST, """{"error":"invalid_grant"}""")
      else -> send(exchange, STATUS_OK, tokenResponse(login))
    }
  }

  private fun tokenResponse(login: PendingLogin): String {
    val now = Instant.now()
    val claims =
        JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(TOKEN_LIFETIME_SECONDS)))
    login.nonce?.let { claims.claim("nonce", it) }
    login.claims.forEach { (name, value) -> claims.claim(name, value) }
    val idToken =
        SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims.build())
    idToken.sign(RSASSASigner(key))
    return JSONObjectUtils.toJSONString(
        mapOf(
            "access_token" to UUID.randomUUID().toString(),
            "token_type" to "Bearer",
            "expires_in" to TOKEN_LIFETIME_SECONDS,
            "id_token" to idToken.serialize()))
  }

  private fun basicCredentials(): String =
      "Basic " + Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

  private fun challenge(verifier: String?): String? =
      verifier?.let {
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(it.toByteArray()))
      }

  private fun respond(exchange: HttpExchange, body: () -> String) {
    if (available) send(exchange, STATUS_OK, body()) else send(exchange, STATUS_UNAVAILABLE, "")
  }

  private fun send(exchange: HttpExchange, status: Int, body: String) {
    try {
      val bytes = body.toByteArray()
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
      exchange.responseBody.write(bytes)
    } finally {
      exchange.close()
    }
  }

  private fun parseQuery(query: String?): Map<String, String> =
      query
          .orEmpty()
          .split("&")
          .filter { it.contains("=") }
          .associate {
            val (name, value) = it.split("=", limit = 2)
            URLDecoder.decode(name, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
          }

  private class PendingLogin(
      val claims: Map<String, Any>,
      val nonce: String?,
      val codeChallenge: String
  )
}
