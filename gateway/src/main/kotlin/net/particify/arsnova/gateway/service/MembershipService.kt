package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.AuthProcessor
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MembershipService(
  private val authProcessor: AuthProcessor,
  private val roomAccessService: RoomAccessService
) {
  fun cancel(roomId: String): Mono<RoomAccess> {
    return authProcessor.getAuthentication()
      .map { authentication ->
        // Most values can be null as the room access service only needs userId and roomId
        RoomAccess(
          roomId,
          authentication.principal.toString(),
          "",
          "",
          null
        )
      }
      .flatMap { roomAccess ->
        roomAccessService.deleteRoomAccess(roomAccess)
      }
  }
}
