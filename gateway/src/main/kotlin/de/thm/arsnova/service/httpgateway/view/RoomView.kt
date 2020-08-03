package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.CommentStats
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomStats
import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.RoomService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class RoomView(
    private val roomService: RoomService,
    private val commentService: CommentService
) {
    fun getSummaries(roomIds: List<String>): Flux<RoomSummary> {
        val rooms: Flux<Room> = roomService.get(roomIds)
        val commentStats: Flux<CommentStats> = commentService.getStats(roomIds)

        return Flux
            .zip(rooms, commentStats)
            .map { tuple2 ->
                RoomSummary(
                    tuple2.t1.id,
                    tuple2.t1.shortId,
                    tuple2.t1.name,
                    RoomStats(
                        0,
                        tuple2.t2.ackCommentCount
                    )
                )
            }
    }
}
