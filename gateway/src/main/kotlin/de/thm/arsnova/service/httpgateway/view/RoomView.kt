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
import reactor.util.function.Tuple3
import java.util.Optional

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
                            .map { tuple3: Tuple3<CommentStats, Int, Optional<Int>> ->
                                tuple3.t3.map { userCount ->
                                    RoomStats(
                                            tuple3.t2,
                                            tuple3.t1.ackCommentCount,
                                            userCount
                                    )
                                }.orElse(
                                        RoomStats(
                                                tuple3.t2,
                                                tuple3.t1.ackCommentCount,
                                                null
                                        )
                                )
                            }
                }
                .zipWith(roomService.get(roomIds))
                .map { tuple2 ->
                    tuple2.t2
                            .map { room ->
                                Optional.of(RoomSummary(
                                        room.id,
                                        room.shortId,
                                        room.name,
                                        tuple2.t1
                                ))
                            }
                            .orElse(Optional.empty())
                }
    }
}
