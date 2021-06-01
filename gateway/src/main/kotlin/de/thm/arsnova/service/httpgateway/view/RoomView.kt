package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.CommentStats
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomStats
import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.ContentService
import de.thm.arsnova.service.httpgateway.service.RoomService
import de.thm.arsnova.service.httpgateway.service.WsGatewayService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3

@Component
class RoomView(
    private val authProcessor: AuthProcessor,
    private val roomService: RoomService,
    private val contentService: ContentService,
    private val commentService: CommentService,
    private val wsGatewayService: WsGatewayService
) {
    fun getSummaries(roomIds: List<String>): Flux<Optional<RoomSummary>> {
        return authProcessor.getAuthentication()
                .map { authentication ->
                    authentication.credentials
                }
                .cast(String::class.java)
                .flatMapMany { jwt: String ->
                    Flux.zip(
                            commentService.getStats(roomIds, jwt),
                            contentService.getStats(roomIds, jwt),
                            wsGatewayService.getUsercount(roomIds)
                    )
                            .map { (commentStats: CommentStats, contentCount: Int, wsUserCount: Optional<Int>) ->
                                wsUserCount.map { userCount ->
                                    RoomStats(
                                            contentCount,
                                            commentStats.ackCommentCount,
                                            userCount
                                    )
                                }.orElse(
                                        RoomStats(
                                                contentCount,
                                                commentStats.ackCommentCount,
                                                null
                                        )
                                )
                            }
                }
                .zipWith(roomService.get(roomIds))
                .map { (roomStats: RoomStats, optionalRoom: Optional<Room>) ->
                    optionalRoom
                            .map { room ->
                                Optional.of(RoomSummary(
                                        room.id,
                                        room.shortId,
                                        room.name,
                                        roomStats
                                ))
                            }
                            .orElse(Optional.empty())
                }
    }
}
