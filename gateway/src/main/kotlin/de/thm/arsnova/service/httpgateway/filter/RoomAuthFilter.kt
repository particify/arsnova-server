package de.thm.arsnova.service.httpgateway.filter

import de.thm.arsnova.service.httpgateway.exception.BadRequestException
import de.thm.arsnova.service.httpgateway.model.AccessChangeRequest
import de.thm.arsnova.service.httpgateway.model.AccessChangeRequestType
import de.thm.arsnova.service.httpgateway.model.AccessLevel
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.security.JwtTokenUtil
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
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
 * This filter is responsible for extracting room access change reuqests and chaining them to the auth service.
 * Whenever access for a room is going to be changed, the core manages the initial request.
 * After successful changes at the core, this filter creates requests to tell the auth service about those changes.
 *
 * Example:
 * When a moderator is added to a room, it will be PUT into the room entity at the core, and afterwards a room
 * access with the role will be created in the auth service.
 */
@Component
class RoomAuthFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val roomAccessService: RoomAccessService
) : AbstractGatewayFilterFactory<RoomAuthFilter.Config>(Config::class.java) {

    companion object {
        const val ENTITY_ID_HEADER = "Arsnova-Entity-Id"
        const val ENTITY_REVISION_HEADER = "Arsnova-Entity-Revision"
        const val BEARER_HEADER = "Bearer "
        const val ROOM_POST_PATH = "/room/"
        val ROOM_REGEX = Regex("/room/[0-9a-f]{32}")
        val ROOM_SHORT_ID_REGEX = Regex("/room/~[0-9]{8}")
        val ROOM_MODERATOR_REGEX = Regex("/room/[0-9a-f]{32}/moderator/[0-9a-f]{32}")
        val ROOM_TRANSFER_REGEX = Regex("/room/[0-9a-f]{32}/transfer")
        const val ROOM_TRANSFER_BY_ID_QUERY_PARAMETER = "newOwnerId"
        const val ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER = "newOwnerToken"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            chain.filter(exchange)
                .then(Mono.just(exchange)
                    .filter { exchange ->
                        exchange.response.statusCode != null &&
                                exchange.response.statusCode!!.is2xxSuccessful &&
                                exchange.request.method != null
                    }
                    .filter { exchange ->
                        exchange.request.method == HttpMethod.POST ||
                                exchange.request.method == HttpMethod.PUT ||
                                exchange.request.method == HttpMethod.PATCH ||
                                exchange.request.method == HttpMethod.DELETE
                    }
                    .map { exchange ->
                        val path = exchange.request.path.toString()
                        val method = exchange.request.method!!
                        val roomId = exchange.response.headers.getFirst(ENTITY_ID_HEADER)!!
                        val revId = exchange.response.headers.getFirst(ENTITY_REVISION_HEADER)!!
                        val token = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)!!.removePrefix(BEARER_HEADER)
                        val userId = jwtTokenUtil.getUserIdFromPublicToken(token)

                        if (path == ROOM_POST_PATH && method == HttpMethod.POST) {
                            listOf(AccessChangeRequest(
                                AccessChangeRequestType.CREATE,
                                roomId,
                                revId,
                                userId,
                                AccessLevel.CREATOR
                            ))
                        } else if ((path.matches(ROOM_REGEX) || path.matches(ROOM_SHORT_ID_REGEX)) && method == HttpMethod.DELETE) {
                            listOf(AccessChangeRequest(
                                AccessChangeRequestType.DELETE_ALL,
                                roomId,
                                revId,
                                userId,
                                AccessLevel.CREATOR
                            ))
                        } else if (path.matches(ROOM_MODERATOR_REGEX) && method == HttpMethod.PUT) {
                            val moderatorId = path.substringAfter("/moderator/")
                            listOf(AccessChangeRequest(
                                AccessChangeRequestType.CREATE,
                                roomId,
                                revId,
                                moderatorId,
                                AccessLevel.EXECUTIVE_MODERATOR
                            ))
                        } else if (path.matches(ROOM_MODERATOR_REGEX) && method == HttpMethod.DELETE) {
                            val moderatorId = path.substringAfter("/moderator/")
                            listOf(AccessChangeRequest(
                                AccessChangeRequestType.DELETE,
                                roomId,
                                revId,
                                moderatorId,
                                AccessLevel.EXECUTIVE_MODERATOR
                            ))
                        } else if (
                                path.matches(ROOM_TRANSFER_REGEX) &&
                                exchange.request.queryParams.containsKey(ROOM_TRANSFER_BY_ID_QUERY_PARAMETER) &&
                                method == HttpMethod.POST
                        ) {
                            val newOwnerId = exchange.request.queryParams[ROOM_TRANSFER_BY_ID_QUERY_PARAMETER]!!.first()
                            listOf(
                                AccessChangeRequest(
                                    AccessChangeRequestType.CREATE,
                                    roomId,
                                    revId,
                                    newOwnerId,
                                    AccessLevel.CREATOR
                                ),
                                AccessChangeRequest(
                                    AccessChangeRequestType.DELETE,
                                    roomId,
                                    revId,
                                    userId,
                                    AccessLevel.CREATOR
                                )
                            )
                        } else if (
                                path.matches(ROOM_TRANSFER_REGEX) &&
                                exchange.request.queryParams.containsKey(ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER) &&
                                method == HttpMethod.POST
                        ) {
                            val newOwnerToken = exchange.request.queryParams[ROOM_TRANSFER_BY_TOKEN_QUERY_PARAMETER]!!.first()
                            val newOwnerId = jwtTokenUtil.getUserIdFromPublicToken(newOwnerToken)
                            listOf(
                                AccessChangeRequest(
                                    AccessChangeRequestType.CREATE,
                                    roomId,
                                    revId,
                                    newOwnerId,
                                    AccessLevel.CREATOR
                                ),
                                AccessChangeRequest(
                                    AccessChangeRequestType.DELETE,
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
                            AccessChangeRequestType.DELETE_ALL -> {
                                roomAccessService.deleteRoomAccessByRoomId(accessChangeRequest.roomId)
                            }
                            else -> Mono.error(BadRequestException())
                        }
                    }
                    .then()
                )
        }
    }

    class Config {
        var name: String = "RoomAuthFilter"
    }
}