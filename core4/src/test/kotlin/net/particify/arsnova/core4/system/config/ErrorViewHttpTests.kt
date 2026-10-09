/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

/**
 * Against a running server, because MockMvc never dispatches to the error page: what a browser sees
 * after an error is decided there.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ErrorViewHttpTests {
  @LocalServerPort var port: Int = 0

  private val client = HttpClient.newHttpClient()

  @Test
  fun shouldAnswerUnknownSsoRegistrationWithNotFoundPage() {
    val response = get("/api/auth/sso/${UUID.randomUUID()}", MediaType.TEXT_HTML_VALUE)
    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode())
    assertErrorPage(response, "404 Not Found")
  }

  @Test
  fun shouldRenderErrorPageInsteadOfWhitelabel() {
    val response = get("/api/auth/sso/not-a-uuid", MediaType.TEXT_HTML_VALUE)
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode())
    assertErrorPage(response, "400 Bad Request")
  }

  @Test
  fun shouldKeepJsonForJsonClients() {
    val response = get("/api/auth/sso/${UUID.randomUUID()}", MediaType.APPLICATION_JSON_VALUE)
    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode())
    Assertions.assertTrue(
        MediaType.APPLICATION_JSON.isCompatibleWith(contentType(response)), response.body())
  }

  private fun assertErrorPage(response: HttpResponse<String>, heading: String) {
    Assertions.assertTrue(MediaType.TEXT_HTML.isCompatibleWith(contentType(response)))
    Assertions.assertTrue(response.body().contains("<h1>$heading</h1>"), response.body())
    Assertions.assertFalse(response.body().contains("Whitelabel"), response.body())
  }

  private fun contentType(response: HttpResponse<String>): MediaType =
      MediaType.parseMediaType(
          response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElseThrow())

  private fun get(path: String, accept: String): HttpResponse<String> =
      client.send(
          HttpRequest.newBuilder(URI("http://localhost:$port$path"))
              .header(HttpHeaders.ACCEPT, accept)
              .build(),
          HttpResponse.BodyHandlers.ofString())
}
