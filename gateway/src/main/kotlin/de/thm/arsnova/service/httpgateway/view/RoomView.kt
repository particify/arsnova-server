package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.CommentStats
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomStats
import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.ContentService
import de.thm.arsnova.service.httpgateway.service.RoomService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional

@Component
class RoomView(
    private val authProcessor: AuthProcessor,
    private val roomService: RoomService,
    private val contentService: ContentService,
    private val commentService: CommentService
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
                            contentService.getStats(roomIds, jwt)
                    )
                            .map { tuple2 ->
                                RoomStats(
                                        tuple2.t2,
                                        tuple2.t1.ackCommentCount
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
