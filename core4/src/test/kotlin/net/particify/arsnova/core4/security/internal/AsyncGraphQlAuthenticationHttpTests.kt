/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

/**
 * A resolver that has not finished when the handler returns makes the server complete the response
 * in a second dispatch of the same request. Virtual threads make resolvers run that way, and only a
 * running server performs that dispatch.
 *
 * A resolver occasionally finishes before the handler returns, which hides the problem for that
 * request, so each case is repeated.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.threads.virtual.enabled=true"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class AsyncGraphQlAuthenticationHttpTests {
  @LocalServerPort var port: Int = 0
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var jsonMapper: JsonMapper
  @Autowired lateinit var userService: UserService

  private val client = HttpClient.newHttpClient()

  @Test
  fun shouldKeepUserAuthenticationForGraphQlResponse() {
    val user = userService.createAccount()
    val token = jwtUtils.encodeJwt(user.id.toString(), user.roles.map { it.name!! })
    repeat(REPETITIONS) {
      val response = postGraphQl(token, "{ currentUser { id } }")
      Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
      Assertions.assertEquals(
          user.id.toString(),
          jsonMapper.readTree(response.body()).at("/data/currentUser/id").asString())
    }
  }

  @Test
  fun shouldKeepChallengeAuthenticationForGraphQlResponse() {
    repeat(REPETITIONS) {
      // A solved challenge is accepted once only.
      val token =
          jwtUtils.encodeJwt("challenge-${UUID.randomUUID()}", listOf(CHALLENGE_SOLVED_ROLE))
      val response =
          postGraphQl(
              token,
              """mutation { requestUserPasswordReset(mailAddress: "unknown@example.com") }""")
      Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
      Assertions.assertEquals(
          "NOT_FOUND",
          jsonMapper.readTree(response.body()).at("/errors/0/extensions/classification").asString())
    }
  }

  private fun postGraphQl(token: String, query: String): HttpResponse<String> =
      client.send(
          HttpRequest.newBuilder(URI("http://localhost:$port/api/graphql"))
              .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(mapOf("query" to query))))
              .build(),
          HttpResponse.BodyHandlers.ofString())

  companion object {
    private const val REPETITIONS = 10
  }
}
