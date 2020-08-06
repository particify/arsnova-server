package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.CommentStats
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomStats
import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.RoomService
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class RoomView(
    private val roomService: RoomService,
    private val commentService: CommentService
) {
    fun getSummaries(roomIds: List<String>): Flux<RoomSummary> {
        return ReactiveSecurityContextHolder.getContext()
                .map { securityContext ->
                    securityContext.authentication.principal
                }
                .cast(String::class.java)
                .flatMapMany { jwt: String ->
                    commentService.getStats(roomIds, jwt)
                }
                .zipWith(roomService.get(roomIds))
                .map { tuple2 ->
                    RoomSummary(
                            tuple2.t2.id,
                            tuple2.t2.shortId,
                            tuple2.t2.name,
                            RoomStats(
                                    0,
                                    tuple2.t1.ackCommentCount
                            )
                    )
                }
    }
}
