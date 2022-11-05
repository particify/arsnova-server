package net.particify.arsnova.gateway.filter

import net.particify.arsnova.gateway.model.AccessChangeRequest
import net.particify.arsnova.gateway.model.AccessChangeRequestType
import net.particify.arsnova.gateway.model.AccessLevel
import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.JwtTokenUtil
import net.particify.arsnova.gateway.service.RoomAccessService
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * This filter is responsible for creating room access for the creator after successful room creation at the core.
 * It sends a request to the auth service create the access entry.
 */
@Component
class AddRoomCreatorAccessFilter(
  private val jwtTokenUtil: JwtTokenUtil,
  private val roomAccessService: RoomAccessService
) : AbstractGatewayFilterFactory<AddRoomCreatorAccessFilter.Config>(Config::class.java) {

  companion object {
    const val ENTITY_ID_HEADER = "Arsnova-Entity-Id"
    const val ENTITY_REVISION_HEADER = "Arsnova-Entity-Revision"
    const val BEARER_HEADER = "Bearer "
    const val ROOM_POST_PATH = "/room/"
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun apply(config: Config): GatewayFilter {
    return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
      chain.filter(exchange)
        .then(
          Mono.just(exchange)
            .filter { e: ServerWebExchange ->
              e.response.statusCode != null &&
                e.response.statusCode!!.is2xxSuccessful &&
                e.request.method === HttpMethod.POST
            }
            .map { e: ServerWebExchange ->
              val path = e.request.path.toString()
              val method = e.request.method!!
              val roomId = e.response.headers.getFirst(ENTITY_ID_HEADER)!!
              val revId = e.response.headers.getFirst(ENTITY_REVISION_HEADER)!!
              val token = e.request.headers.getFirst(HttpHeaders.AUTHORIZATION)!!.removePrefix(BEARER_HEADER)
              val userId = jwtTokenUtil.getUserIdFromPublicToken(token)

              if (path == ROOM_POST_PATH && method === HttpMethod.POST) {
                listOf(
                  AccessChangeRequest(
                    AccessChangeRequestType.CREATE,
                    roomId,
                    revId,
                    userId,
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
                else -> throw IllegalStateException("Unexpected AccessChangeRequestType")
              }
            }
            .then()
        )
    }
  }

  class Config {
    var name: String = "RoomCreationAuthFilter"
  }
}
