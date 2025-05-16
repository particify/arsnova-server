package net.particify.arsnova.websocket.service

import net.particify.arsnova.websocket.adapter.AuthChannelInterceptorAdapter
import net.particify.arsnova.websocket.config.WebSocketProperties
import net.particify.arsnova.websocket.model.RoomAccess
import net.particify.arsnova.websocket.security.JwtTokenUtil
import net.particify.arsnova.websocket.security.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

@Service
class RoomAccessService(
  private val webSocketProperties: WebSocketProperties,
  private val restTemplate: RestTemplate,
  private val jwtTokenUtil: JwtTokenUtil,
) {
  companion object {
    const val OWNER_ROLE_PREFIX = "OWNER-"
    const val EDITOR_ROLE_PREFIX = "EDITOR-"
    const val MODERATOR_ROLE_PREFIX = "MODERATOR-"
    const val ROOM_ACCESS_STRING = "roomaccess"
  }

  private val logger = LoggerFactory.getLogger(RoomAccessService::class.java)
  private var roomAccessGetEndpoint = "${webSocketProperties.httpClient.authService}/$ROOM_ACCESS_STRING"
  private val jwtAuthorizationClient =
    RestClient.builder()
      .baseUrl(webSocketProperties.httpClient.authService + webSocketProperties.security.authorizeUriEndpoint)
      .build()

  fun getRoomAccess(
    roomId: String,
    user: User,
  ): RoomAccess {
    return if (webSocketProperties.httpClient.useJwtEndpoint) {
      getRoomAccessForJwt(roomId, user.jwt)
    } else {
      getRoomAccessForUserId(roomId, user.userId)
    }
  }

  /**
   * Check for room roles using the user ID. This is implementation depends on the authz service.
   */
  private fun getRoomAccessForUserId(
    roomId: String,
    userId: String,
  ): RoomAccess {
    val url = "$roomAccessGetEndpoint/{roomId}/{userId}"
    logger.trace("Querying auth service for room access with url: {}", url)
    try {
      return restTemplate.getForObject(
        url,
        RoomAccess::class.java,
        mapOf(
          "roomId" to roomId,
          "userId" to userId,
        ),
      )
        ?: RoomAccess(roomId, userId, "", AuthChannelInterceptorAdapter.participantRoleString)
    } catch (e: HttpClientErrorException.NotFound) {
      return RoomAccess(roomId, userId, "", AuthChannelInterceptorAdapter.participantRoleString)
    }
  }

  /**
   * Check for room roles using a JWT. This implementation is compatible with the authz service and the new core.
   */
  private fun getRoomAccessForJwt(
    roomId: String,
    jwt: String,
  ): RoomAccess {
    val userId = jwtTokenUtil.getUser(jwt).userId
    val authorization =
      jwtAuthorizationClient.get()
        .header("Authorization", "Bearer $jwt")
        .header(
          webSocketProperties.security.authorizeUriHeader,
          "${webSocketProperties.security.authorizeUriPrefix}/room/$roomId",
        )
        .retrieve()
        .toBodilessEntity()
        .headers["authorization"]?.first() ?: error("No room authorization received.")
    val roles = jwtTokenUtil.getRolesFromToken(authorization.removePrefix("Bearer "))
    val role =
      when {
        roles.contains(OWNER_ROLE_PREFIX + roomId) -> "OWNER"
        roles.contains(EDITOR_ROLE_PREFIX + roomId) -> "EDITOR"
        roles.contains(MODERATOR_ROLE_PREFIX + roomId) -> "MODERATOR"
        else -> "PARTICIPANT"
      }
    return RoomAccess(roomId, userId, role = role)
  }
}
