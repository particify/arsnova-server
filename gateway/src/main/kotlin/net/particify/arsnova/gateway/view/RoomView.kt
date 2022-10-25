package net.particify.arsnova.gateway.view

import net.particify.arsnova.gateway.model.CommentStats
import net.particify.arsnova.gateway.model.Room
import net.particify.arsnova.gateway.model.RoomStats
import net.particify.arsnova.gateway.model.RoomSummary
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.CommentService
import net.particify.arsnova.gateway.service.ContentService
import net.particify.arsnova.gateway.service.RoomService
import net.particify.arsnova.gateway.service.WsGatewayService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3
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
            Optional.of(
              RoomSummary(
                room.id,
                room.shortId,
                room.name,
                roomStats
              )
            )
          }
          .orElse(Optional.empty())
      }
  }
}
