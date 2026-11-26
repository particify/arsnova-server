/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

private const val PASSWORD = "sXQodP0UYeGQXuXE"

/**
 * HTTP Basic is enabled under the `dev` profile only. The profile is added once the configuration
 * files are loaded, so that the dev configuration, which points to services of the development
 * environment, is not.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [HttpBasicAuthenticationHttpTests.DevProfile::class])
@Import(TestcontainersConfiguration::class)
class HttpBasicAuthenticationHttpTests {
  @LocalServerPort var port: Int = 0
  @Autowired lateinit var jsonMapper: JsonMapper
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var userService: UserServiceImpl

  private val client = HttpClient.newHttpClient()

  @Test
  fun shouldResolveTheAuthenticatedUser() {
    val user = createLocalAccount()
    val body = postGraphQl(user, "{ currentUser { id } }")
    Assertions.assertEquals(user.id.toString(), body.at("/data/currentUser/id").asString())
  }

  @Test
  fun shouldGrantTheAuthoritiesOfAnAccessToken() {
    val user = createLocalAccount()
    val body = postGraphQl(user, """mutation { createRoom(input: { name: "Basic" }) { id } }""")
    Assertions.assertTrue(body.at("/errors").isMissingNode, body.toString())
    Assertions.assertFalse(body.at("/data/createRoom/id").isMissingNode, body.toString())
  }

  private fun createLocalAccount(): User {
    val user = userService.createAccount()
    val mailAddress = "basic-${UUID.randomUUID()}@example.com"
    user.username = mailAddress
    user.mailAddress = mailAddress
    user.password = passwordEncoder.encode(PASSWORD)
    return userService.save(user)
  }

  private fun postGraphQl(user: User, query: String): JsonNode {
    val credentials = "${user.username}:$PASSWORD"
    val response =
        client.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/api/graphql"))
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic ${Base64.getEncoder().encodeToString(credentials.toByteArray())}")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        jsonMapper.writeValueAsString(mapOf("query" to query))))
                .build(),
            HttpResponse.BodyHandlers.ofString())
    Assertions.assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
    return jsonMapper.readTree(response.body())
  }

  class DevProfile : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
      applicationContext.environment.addActiveProfile("dev")
    }
  }
}
