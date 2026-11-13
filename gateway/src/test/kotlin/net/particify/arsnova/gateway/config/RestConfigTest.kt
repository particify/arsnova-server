package net.particify.arsnova.gateway.config

import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.RoomAccessService
import net.particify.arsnova.gateway.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.mock.http.client.reactive.MockClientHttpResponse
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import java.util.Date

class RestConfigTest {
  private val requestBodies = mutableListOf<String>()
  private val recordingConnector =
    ClientHttpConnector { method, uri, requestCallback ->
      val request = MockClientHttpRequest(method, uri)
      requestCallback
        .apply(request)
        .then(Mono.defer { request.bodyAsString })
        .map { body ->
          requestBodies.add(body)
          MockClientHttpResponse(HttpStatus.NO_CONTENT)
        }
    }
  private val httpGatewayProperties =
    mock(HttpGatewayProperties::class.java).also {
      given(it.httpClient).willReturn(HttpClient("http://authz", "http://comments", "http://core", "http://ws", null))
    }
  private val webClient =
    RestConfig(httpGatewayProperties)
      .authServiceWebClient()!!
      .mutate()
      .clientConnector(recordingConnector)
      .build()

  @Test
  fun testShouldSendAnnouncementReadTimestampAsEpochMillis() {
    val userService = UserService(webClient, httpGatewayProperties, mock(AuthProcessor::class.java))

    val before = System.currentTimeMillis()
    userService.updateAnnouncementReadTimestamp("user-id", "jwt").block()

    val timestamp = JsonMapper().readTree(requestBodies.single())["announcementReadTimestamp"]
    assertTrue(timestamp.isIntegralNumber)
    assertTrue(timestamp.asLong() in before..System.currentTimeMillis())
  }

  @Test
  fun testShouldSendRoomAccessLastAccessAsEpochMillis() {
    val roomAccessService = RoomAccessService(webClient, httpGatewayProperties)
    val roomAccess = RoomAccess("room-id", "user-id", "1-0", "PARTICIPANT", Date(1_700_000_000_000))

    roomAccessService.postRoomAccess(roomAccess).block()

    val lastAccess = JsonMapper().readTree(requestBodies.single())["lastAccess"]
    assertTrue(lastAccess.isIntegralNumber)
    assertEquals(1_700_000_000_000, lastAccess.asLong())
  }
}
