package net.particify.arsnova.gateway.filter

import net.particify.arsnova.gateway.model.AccessChangeRequest
import net.particify.arsnova.gateway.model.AccessChangeRequestType
import net.particify.arsnova.gateway.model.AccessLevel
import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.JwtTokenUtil
import net.particify.arsnova.gateway.service.RoomAccessService
import net.particify.arsnova.gateway.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * This filter is responsible for extracting room access change requests and chaining them to the auth service.
 * It handles access changes, which do not require any interaction with the core.
 * This includes adding or removing moderators and transferring room ownership.
 */
@Component
class UpdateRoomAccessFilter(
  private val jwtTokenUtil: JwtTokenUtil,
  private val roomAccessService: RoomAccessService,
  private val userService: UserService
) : AbstractGatewayFilterFactory<UpdateRoomAccessFilter.Config>(Config::class.java) {

  companion object {
    const val BEARER_HEADER = "Bearer "
    const val REV_ID_FALLBACK = "1-0"
    val ROOM_MODERATOR_REGEX = Regex("/room/[0-9a-f]{32}/moderator/[0-9a-f]{32}")
    val ROOM_TRANSFER_REGEX = Regex("/room/[0-9a-f]{32}/transfer")
    const val ROOM_TRANSFER_BY_ID_QUERY_PARAMETER = "newOwnerId"
    const val ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER = "newOwnerToken"
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun apply(config: Config): GatewayFilter {
    return GatewayFilter { exchange: ServerWebExchange, _ ->
      val request = exchange.request
      val path = request.path.toString()
      val method = request.method!!
      val token = request.headers.getFirst(HttpHeaders.AUTHORIZATION)!!.removePrefix(BEARER_HEADER)
      val userId = jwtTokenUtil.getUserIdFromPublicToken(token)
      val uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange)
      val roomId = uriVariables["roomId"]!!

      val accessLevels = jwtTokenUtil.getAccessLevelsFromInternalTokenForRoom(token, roomId)
      if (!accessLevels.contains(AccessLevel.CREATOR) && !jwtTokenUtil.isAdmin(token)) {
        logger.trace("User's access levels for room {}: {}", roomId, accessLevels)
        throw ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "'CREATOR' access level required to update room roles."
        )
      }

      Mono.just(exchange.request)
        .filter { r: ServerHttpRequest ->
          r.method == HttpMethod.POST ||
            r.method == HttpMethod.PUT ||
            r.method == HttpMethod.PATCH ||
            r.method == HttpMethod.DELETE
        }
        .map {
          if (path.matches(ROOM_MODERATOR_REGEX) && method == HttpMethod.PUT) {
            val moderatorId = path.substringAfter("/moderator/")
            if (moderatorId == userId) {
              throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change own access level")
            }
            listOf(
              AccessChangeRequest(
                AccessChangeRequestType.CREATE,
                roomId,
                REV_ID_FALLBACK,
                moderatorId,
                AccessLevel.EXECUTIVE_MODERATOR
              )
            )
          } else if (path.matches(ROOM_MODERATOR_REGEX) && method == HttpMethod.DELETE) {
            val moderatorId = path.substringAfter("/moderator/")
            if (moderatorId == userId) {
              throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change own access level")
            }
            listOf(
              AccessChangeRequest(
                AccessChangeRequestType.DELETE,
                roomId,
                REV_ID_FALLBACK,
                moderatorId,
                AccessLevel.EXECUTIVE_MODERATOR
              )
            )
          } else if (
            path.matches(ROOM_TRANSFER_REGEX) &&
            request.queryParams.containsKey(ROOM_TRANSFER_BY_ID_QUERY_PARAMETER) &&
            method == HttpMethod.POST
          ) {
            val newOwnerId = request.queryParams[ROOM_TRANSFER_BY_ID_QUERY_PARAMETER]!!.first()
            listOf(
              AccessChangeRequest(
                AccessChangeRequestType.CREATE,
                roomId,
                REV_ID_FALLBACK,
                newOwnerId,
                AccessLevel.CREATOR
              )
            )
          } else if (
            path.matches(ROOM_TRANSFER_REGEX) &&
            request.queryParams.containsKey(ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER) &&
            method == HttpMethod.POST
          ) {
            val newOwnerToken = request.queryParams[ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER]!!.first()
            val newOwnerId = jwtTokenUtil.getUserIdFromPublicToken(newOwnerToken)
            listOf(
              AccessChangeRequest(
                AccessChangeRequestType.CREATE,
                roomId,
                REV_ID_FALLBACK,
                newOwnerId,
                AccessLevel.CREATOR
              )
            )
          } else {
            listOf()
          }
        }
        .flatMapMany { list: List<AccessChangeRequest> ->
          Flux.fromIterable(list)
        }
        .flatMap { accessChangeRequest: AccessChangeRequest ->
          userService.exists(accessChangeRequest.userId, token)
            .map { exists ->
              if (!exists) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID does not exist.")
              }
              accessChangeRequest
            }
        }
        .flatMap { accessChangeRequest: AccessChangeRequest ->
          when (accessChangeRequest.type) {
            AccessChangeRequestType.CREATE -> {
              val roomAccess = RoomAccess(
                accessChangeRequest.roomId,
                accessChangeRequest.userId,
                accessChangeRequest.revId,
                accessChangeRequest.level.name,
                null
              )
              roomAccessService.postRoomAccess(roomAccess)
            }
            AccessChangeRequestType.DELETE -> {
              val roomAccess = RoomAccess(
                accessChangeRequest.roomId,
                accessChangeRequest.userId,
                accessChangeRequest.revId,
                accessChangeRequest.level.name,
                null
              )
              roomAccessService.deleteRoomAccess(roomAccess)
            }
            else -> throw IllegalStateException("Unexpected AccessChangeRequestType")
          }
        }
        .then()
    }
  }

  class Config {
    var name: String = "RoomAuthFilter"
  }
}
