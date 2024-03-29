package net.particify.arsnova.gateway.controller

import net.particify.arsnova.gateway.model.RoomRole
import net.particify.arsnova.gateway.service.RoomAccessService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
class RoomModeratorController(
  private val roomAccessService: RoomAccessService,
) {
  companion object {
    const val BASE_MAPPING = "/room/{roomId}/moderator"
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  @GetMapping(path = [BASE_MAPPING])
  @ResponseBody
  fun getRoomModerators(
    @PathVariable roomId: String,
  ): Flux<RoomRole> {
    logger.trace("Getting moderators for room: {}", roomId)
    return roomAccessService.getRoomModerators(roomId).map { roomAccess ->
      RoomRole(roomAccess.userId, roomAccess.role)
    }
  }
}
