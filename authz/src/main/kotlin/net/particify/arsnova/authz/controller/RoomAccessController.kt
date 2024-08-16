package net.particify.arsnova.authz.controller

import net.particify.arsnova.authz.exception.NotFoundException
import net.particify.arsnova.authz.handler.RoomAccessHandler
import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.RoomAccessSyncTracker
import net.particify.arsnova.authz.model.command.RequestRoomAccessSyncCommand
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
import java.time.Instant
import java.util.Date
import java.util.Optional
import java.util.UUID

@Controller
class RoomAccessController(
  private val handler: RoomAccessHandler,
) {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  @GetMapping(path = ["/roomaccess/by-user/{userId}"])
  @ResponseBody
  fun getRoomAccessByUser(
    @PathVariable userId: UUID,
  ): Flux<RoomAccess> {
    return Flux.fromIterable(handler.getByUserId(userId))
  }

  @GetMapping(path = ["/roomaccess/owner/by-room"])
  @ResponseBody
  fun getOwnerByRoomIds(
    @RequestParam ids: List<UUID>,
  ): Flux<Optional<RoomAccess>> {
    return Flux.fromIterable(
      ids.map { id ->
        Optional.ofNullable(handler.getOwnerRoomAccessByRoomId(id))
      },
    )
  }

  @GetMapping(path = ["/roomaccess/by-room/{roomId}"])
  @ResponseBody
  fun getByRoomId(
    @PathVariable roomId: UUID,
    @RequestParam role: String?,
  ): Flux<RoomAccess> {
    val roomAccess = handler.getByRoomIdAndRole(roomId, role)
    return Flux.fromIterable(roomAccess)
  }

  @GetMapping(path = ["/roomaccess/{roomId}/{userId}"])
  @ResponseBody
  fun getRoomAccess(
    @PathVariable roomId: UUID,
    @PathVariable userId: UUID,
  ): Mono<RoomAccess> {
    return Mono.just(handler.getByRoomIdAndUserId(roomId, userId))
      .flatMap { optional ->
        Mono.justOrEmpty(optional)
      }
      .switchIfEmpty(Mono.error(NotFoundException()))
  }

  @GetMapping(path = ["/roomaccess/inactive-user-ids"])
  @ResponseBody
  fun getInactiveUserIds(
    @RequestParam lastActiveBefore: String,
  ): Mono<List<UUID>> {
    val lastActiveBeforeInstant = Date.from(Instant.parse(lastActiveBefore))
    return Mono.just(handler.getUserIdsLastActiveBefore(lastActiveBeforeInstant).toList())
  }

  @PostMapping(path = ["/roomaccess/sync/{roomId}/{revNumber}"])
  @ResponseBody
  fun postRequestSync(
    @PathVariable roomId: UUID,
    @PathVariable revNumber: Int,
  ): Mono<RoomAccessSyncTracker> {
    return Mono.just(handler.handleRequestRoomAccessSyncCommand(RequestRoomAccessSyncCommand(roomId, revNumber)))
  }

  @PostMapping(path = ["/roomaccess/"])
  @ResponseBody
  fun create(
    @RequestBody roomAccess: RoomAccess,
    @RequestParam roomParticipantLimit: Int?,
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
    @PathVariable roomId: UUID,
    @PathVariable userId: UUID,
  ): Mono<Unit> {
    return Mono.just(handler.delete(roomId, userId))
  }

  @DeleteMapping(path = ["/roomaccess/{roomId}"])
  @ResponseBody
  fun deleteByRoomId(
    @PathVariable roomId: UUID,
  ): Flux<RoomAccess> {
    return Flux.fromIterable(handler.deleteByRoomId(roomId))
  }
}
