package de.thm.arsnova.service.httpgateway.controller

import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
class RoomModeratorController(
    private val roomAccessService: RoomAccessService
) {

    companion object {
        const val baseMapping = "/room/{roomId}/moderator"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(path = [baseMapping])
    @ResponseBody
    fun getRoomModerators(
        @PathVariable roomId: String
    ): Flux<RoomAccess> {
        logger.trace("Getting moderators for room: {}", roomId)
        return roomAccessService.getRoomModerators(roomId)
    }
}
