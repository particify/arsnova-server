package de.thm.arsnova.service.authservice.controller

import de.thm.arsnova.service.authservice.exception.NotFoundException
import de.thm.arsnova.service.authservice.handler.RoomAccessHandler
import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessSyncTracker
import de.thm.arsnova.service.authservice.model.command.RequestRoomAccessSyncCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional

@Controller
class RoomAccessController(
  private val handler: RoomAccessHandler
) {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  @GetMapping(path = ["/roomaccess/by-user/{userId}"])
  @ResponseBody
  fun getRoomAccessByUser(
    @PathVariable userId: String
  ): Flux<RoomAccess> {
    return Flux.fromIterable(handler.getByUserId(userId))
  }

  @GetMapping(path = ["/roomaccess/owner/by-room"])
  @ResponseBody
  fun getOwnerByRoomIds(
    @RequestParam ids: List<String>
  ): Flux<Optional<RoomAccess>> {
    return Flux.fromIterable(
      ids.map { id ->
        val maybeAccess = handler.getOwnerRoomAccessByRoomId(id)
        if (maybeAccess != null) {
          Optional.of(maybeAccess)
        } else {
          Optional.empty()
        }
      }
    )
  }

  @GetMapping(path = ["/roomaccess/by-room/{roomId}"])
  @ResponseBody
  fun getByRoomId(
    @PathVariable roomId: String,
    @RequestParam role: String?
  ): Flux<RoomAccess> {
    val roomAccess = handler.getByRoomIdAndRole(roomId, role)
    return Flux.fromIterable(roomAccess)
  }

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

  @PostMapping(path = ["/roomaccess/"])
  @ResponseBody
  fun create(
    @RequestBody roomAccess: RoomAccess,
    @RequestParam roomParticipantLimit: Int?
  ): Mono<RoomAccess> {
    return if (roomParticipantLimit != null && roomAccess.role == "PARTICIPANT") {
      Mono.just(handler.createParticipantAccessWithLimit(roomAccess, roomParticipantLimit))
    } else {
      Mono.just(handler.create(roomAccess))
    }
  }

  @DeleteMapping(path = ["/roomaccess/{roomId}/{userId}"])
  @ResponseBody
  fun delete(
    @PathVariable roomId: String,
    @PathVariable userId: String
  ): Mono<Unit> {
    return Mono.just(handler.delete(roomId, userId))
  }

  @DeleteMapping(path = ["/roomaccess/{roomId}"])
  @ResponseBody
  fun deleteByRoomId(
    @PathVariable roomId: String
  ): Flux<RoomAccess> {
    return Flux.fromIterable(handler.deleteByRoomId(roomId))
  }
}