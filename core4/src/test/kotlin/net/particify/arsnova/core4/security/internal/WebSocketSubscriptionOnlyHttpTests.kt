/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.UUID
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

/** Account from the `dev` Liquibase fixtures which owns [OWNED_ROOM_ID] and its [QNA_ID]. */
private val OWNER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val OWNED_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")
private val QNA_ID = UUID.fromString("4664a865-2413-4cc7-bfb8-349dd37ade5c")

private const val CURRENT_USER_QUERY = "{ currentUser { id } }"
private const val RECEIVE_TIMEOUT_SECONDS = 10L
private const val EVENT_POLL_MILLIS = 500L
private const val EVENT_ATTEMPTS = 20

/** Against a running server, because the restriction applies to the WebSocket transport only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class WebSocketSubscriptionOnlyHttpTests {
  @LocalServerPort var port: Int = 0
  @Autowired lateinit var jwtUtils: JwtUtils

  private val client = HttpClient.newHttpClient()
  private val jsonMapper = JsonMapper.builder().build()
  private val messages = LinkedBlockingQueue<Map<*, *>>()
  private lateinit var webSocket: WebSocket

  @AfterEach
  fun closeWebSocket() {
    if (::webSocket.isInitialized) {
      webSocket.abort()
    }
  }

  @Test
  fun shouldRejectQueryOverWebSocket() {
    connect()
    subscribe("1", CURRENT_USER_QUERY)
    assertRejected("1")
  }

  @Test
  fun shouldRejectMutationOverWebSocket() {
    connect()
    subscribe("1", "mutation { startQna(id: \"$QNA_ID\") { id } }")
    assertRejected("1")
  }

  @Test
  fun shouldDeliverSubscriptionOverWebSocket() {
    connect()
    subscribe("1", "subscription { qnaStateChanged(roomId: \"$OWNED_ROOM_ID\") { id } }")
    // The subscription becomes active asynchronously, so an event published before then is lost.
    val message =
        (1..EVENT_ATTEMPTS).firstNotNullOfOrNull {
          postGraphql("mutation { startQna(id: \"$QNA_ID\") { id } }")
          messages.poll(EVENT_POLL_MILLIS, TimeUnit.MILLISECONDS)
        }
    Assertions.assertNotNull(message, "No subscription event received")
    Assertions.assertEquals("next", message!!["type"])
    Assertions.assertEquals(
        QNA_ID.toString(), path(message, "payload", "data", "qnaStateChanged", "id"))
  }

  @Test
  fun shouldAnswerQueryOverHttp() {
    val response = postGraphql(CURRENT_USER_QUERY)
    Assertions.assertEquals(200, response.statusCode(), response.body())
    val body = jsonMapper.readValue(response.body(), Map::class.java)
    Assertions.assertNull(body["errors"], response.body())
    Assertions.assertEquals(OWNER_ID.toString(), path(body, "data", "currentUser", "id"))
  }

  private fun assertRejected(id: String) {
    val next = receive()
    Assertions.assertEquals("next", next["type"], next.toString())
    Assertions.assertEquals(id, next["id"])
    Assertions.assertNull(path(next, "payload", "data"), next.toString())
    val error = (path(next, "payload", "errors") as List<*>).single() as Map<*, *>
    Assertions.assertEquals("BAD_REQUEST", path(error, "extensions", "classification"))
    Assertions.assertEquals("complete", receive()["type"])
  }

  private fun connect() {
    webSocket =
        client
            .newWebSocketBuilder()
            .subprotocols("graphql-transport-ws")
            .buildAsync(URI("ws://localhost:$port/api/graphql/ws"), Listener())
            .get(RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    send(
        mapOf(
            "type" to "connection_init",
            "payload" to mapOf(HttpHeaders.AUTHORIZATION to "Bearer ${token()}")))
    Assertions.assertEquals("connection_ack", receive()["type"])
  }

  private fun subscribe(id: String, query: String) =
      send(mapOf("id" to id, "type" to "subscribe", "payload" to mapOf("query" to query)))

  private fun send(message: Map<String, Any>) {
    webSocket.sendText(jsonMapper.writeValueAsString(message), true).get()
  }

  private fun receive(): Map<*, *> =
      checkNotNull(messages.poll(RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        "No WebSocket message received"
      }

  private fun postGraphql(query: String): HttpResponse<String> =
      client.send(
          HttpRequest.newBuilder(URI("http://localhost:$port/api/graphql"))
              .header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(mapOf("query" to query))))
              .build(),
          HttpResponse.BodyHandlers.ofString())

  private fun token(): String = jwtUtils.encodeJwt(OWNER_ID.toString(), listOf())

  private fun path(map: Map<*, *>, vararg keys: String): Any? =
      keys.fold(map as Any?) { current, key -> (current as? Map<*, *>)?.get(key) }

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
  }
}
