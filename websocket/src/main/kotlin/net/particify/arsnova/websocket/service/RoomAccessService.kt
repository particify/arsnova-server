package net.particify.arsnova.websocket.service

import net.particify.arsnova.websocket.adapter.AuthChannelInterceptorAdapter
import net.particify.arsnova.websocket.config.WebSocketProperties
import net.particify.arsnova.websocket.model.RoomAccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class RoomAccessService(
  private val webSocketProperties: WebSocketProperties,
  private val restTemplate: RestTemplate
) {
  companion object {
    val roomAccessString = "roomaccess"
  }

  private val logger = LoggerFactory.getLogger(RoomAccessService::class.java)

  private var roomAccessGetEndpoint = "${webSocketProperties.httpClient.authService}/$roomAccessString"

  fun getRoomAccess(roomId: String, userId: String): RoomAccess {
    val url = "$roomAccessGetEndpoint/{roomId}/{userId}"
    logger.trace("Querying auth service for room access with url: {}", url)
    try {
      return restTemplate.getForObject(
        url, RoomAccess::class.java,
        mapOf(
          "roomId" to roomId,
          "userId" to userId
        )
      )
        ?: RoomAccess(roomId, userId, "", AuthChannelInterceptorAdapter.participantRoleString)
    } catch (e: HttpClientErrorException.NotFound) {
      return RoomAccess(roomId, userId, "", AuthChannelInterceptorAdapter.participantRoleString)
    }
  }
}
