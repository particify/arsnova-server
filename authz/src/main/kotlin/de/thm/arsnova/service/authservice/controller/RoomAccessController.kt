package de.thm.arsnova.service.authservice.controller

import de.thm.arsnova.service.authservice.exception.NotFoundException
import de.thm.arsnova.service.authservice.handler.RoomAccessHandler
import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessSyncTracker
import de.thm.arsnova.service.authservice.model.command.RequestRoomAccessSyncCommand
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono

@Controller
class RoomAccessController (
        private val handler: RoomAccessHandler
) {
    @GetMapping(path = ["/roomaccess/{roomId}/{userId}"])
    @ResponseBody
    fun getRoomAccess(
            @PathVariable roomId: String,
            @PathVariable userId: String
    ): Mono<RoomAccess> {
        return Mono.just(handler.getByRoomIdAndUserId(roomId, userId))
            .flatMap { optional ->
                Mono.justOrEmpty(optional)
            }
            .switchIfEmpty(Mono.error(NotFoundException()))
    }

    @PostMapping(path = ["/roomaccess/sync/{roomId}/{revNumber}"])
    @ResponseBody
    fun postRequestSync(
            @PathVariable roomId: String,
            @PathVariable revNumber: Int
    ): Mono<RoomAccessSyncTracker> {
        return Mono.just(handler.handleRequestRoomAccessSyncCommand(RequestRoomAccessSyncCommand(roomId, revNumber)))
    }
}
