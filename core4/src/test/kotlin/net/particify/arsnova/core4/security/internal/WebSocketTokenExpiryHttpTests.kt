/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

/** Account from the `dev` Liquibase fixtures which owns [OWNED_ROOM_ID]. */
private val OWNER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val OWNED_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

private val QNA_ID = UUID.fromString("4664a865-2413-4cc7-bfb8-349dd37ade5c")

private const val TOKEN_EXPIRED_CODE = 4403
private const val TIMEOUT_SECONDS = 10L
private const val EVENT_POLL_MILLIS = 500L
private const val EVENT_ATTEMPTS = 10

/** Long enough that `exp` lies in the past while the decoder still accepts the token. */
private val VALIDITY = Duration.ofSeconds(120)

/** How much of the decoder's leeway is left when the token is presented. */
private val CLOSE_DELAY = Duration.ofSeconds(3)
private val CLOSE_TOLERANCE = Duration.ofSeconds(2)

/** Against a running server, because what is tested is how the WebSocket session ends. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class WebSocketTokenExpiryHttpTests {
  @LocalServerPort var port: Int = 0
  @Autowired lateinit var jwtUtils: JwtUtils

  private val client = HttpClient.newHttpClient()
  private val jsonMapper = JsonMapper.builder().build()
  private val messages = LinkedBlockingQueue<Map<*, *>>()
  private val closed = CompletableFuture<Pair<Int, Instant>>()
  private lateinit var webSocket: WebSocket

  @AfterEach
  fun closeWebSocket() {
    if (::webSocket.isInitialized) {
      webSocket.abort()
    }
  }

  @Test
  fun shouldCloseSessionWhenDecoderLeewayEnds() {
    val closesAt = Instant.now().plus(CLOSE_DELAY).truncatedTo(ChronoUnit.SECONDS)
    connect(closesAt.minus(JWT_CLOCK_SKEW))
    Assertions.assertEquals("connection_ack", receive()["type"])
    subscribe()

    val (code, closedAt) = closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    Assertions.assertEquals(TOKEN_EXPIRED_CODE, code)
    Assertions.assertFalse(closedAt.isBefore(closesAt), "Closed at $closedAt before $closesAt")
    Assertions.assertTrue(
        closedAt.isBefore(closesAt.plus(CLOSE_TOLERANCE)), "Closed at $closedAt after $closesAt")
  }

  @Test
  fun shouldSubscribeWithExpiredTokenWithinDecoderLeeway() {
    connect(Instant.now().minus(JWT_CLOCK_SKEW).plus(CLOSE_DELAY.multipliedBy(3)))
    Assertions.assertEquals("connection_ack", receive()["type"])
    subscribe()

    // The subscription becomes active asynchronously, so an event published before then is lost.
    val message =
        (1..EVENT_ATTEMPTS).firstNotNullOfOrNull {
          postGraphql("mutation { startQna(id: \"$QNA_ID\") { id } }")
          messages.poll(EVENT_POLL_MILLIS, TimeUnit.MILLISECONDS)
        }
    Assertions.assertEquals("next", message?.get("type"), message.toString())
    Assertions.assertFalse(closed.isDone, "Closed with ${closed.getNow(null)}")
  }

  private fun connect(expiresAt: Instant) {
    webSocket =
        client
            .newWebSocketBuilder()
            .subprotocols("graphql-transport-ws")
            .buildAsync(URI("ws://localhost:$port/api/graphql/ws"), Listener())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    // The decoder rejects a token issued after it expired.
    val issuedAt = Date.from(expiresAt.minus(VALIDITY))
    val token =
        jwtUtils.encodeJwt(
            OWNER_ID.toString(), listOf(), mapOf("iat" to issuedAt), expirationTime = expiresAt)
    send(
        mapOf(
            "type" to "connection_init",
            "payload" to mapOf(HttpHeaders.AUTHORIZATION to "Bearer $token")))
  }

  private fun subscribe() =
      send(
          mapOf(
              "id" to "1",
              "type" to "subscribe",
              "payload" to
                  mapOf(
                      "query" to
                          "subscription { qnaStateChanged(roomId: \"$OWNED_ROOM_ID\") { id } }")))

  private fun postGraphql(query: String): HttpResponse<String> =
      client.send(
          HttpRequest.newBuilder(URI("http://localhost:$port/api/graphql"))
              .header(
                  HttpHeaders.AUTHORIZATION,
                  "Bearer ${jwtUtils.encodeJwt(OWNER_ID.toString(), listOf())}")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(mapOf("query" to query))))
              .build(),
          HttpResponse.BodyHandlers.ofString())

  private fun send(message: Map<String, Any>) {
    webSocket.sendText(jsonMapper.writeValueAsString(message), true).get()
  }

  private fun receive(): Map<*, *> =
      checkNotNull(messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        "No WebSocket message received"
      }

  private inner class Listener : WebSocket.Listener {
    private val buffer = StringBuilder()

    override fun onText(
        webSocket: WebSocket,
        data: CharSequence,
        last: Boolean
    ): CompletionStage<*>? {
      buffer.append(data)
      if (last) {
        messages.add(jsonMapper.readValue(buffer.toString(), Map::class.java))
        buffer.setLength(0)
      }
      return super.onText(webSocket, data, last)
    }

    override fun onClose(
        webSocket: WebSocket,
        statusCode: Int,
        reason: String
    ): CompletionStage<*>? {
      closed.complete(statusCode to Instant.now())
      return null
    }
  }
}
