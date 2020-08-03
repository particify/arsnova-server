package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.Membership
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import de.thm.arsnova.service.httpgateway.service.UserService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class MembershipView(
    private val roomAccessService: RoomAccessService,
    private val userService: UserService
) {
    fun getByUser(userId: String): Flux<Membership> {
        val roomAccesses: Flux<RoomAccess> = roomAccessService.getRoomAccessByUser(userId)
        val visitedRooms: Flux<RoomHistoryEntry> = userService.getRoomHistory(userId)

        val membershipFromAccess: Flux<Membership> = roomAccesses.map { roomAccess ->
            Membership(
                roomAccess.roomId!!,
                "shortId",
                listOf(roomAccess.role!!),
                "lastVisit"
            )
        }

        val membershipFromHistory: Flux<Membership> = visitedRooms.map { roomHistoryEntry ->
            Membership(
                roomHistoryEntry.roomId,
                "shortId",
                listOf("GUEST"),
                roomHistoryEntry.lastVisit
            )
        }

        return Flux.concat(membershipFromAccess, membershipFromHistory)
    }
}
