package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
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
