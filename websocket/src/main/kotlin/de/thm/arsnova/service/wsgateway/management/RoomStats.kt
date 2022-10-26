package de.thm.arsnova.service.wsgateway.management

import de.thm.arsnova.service.wsgateway.service.RoomSubscriptionService
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.stereotype.Component

@Component
@Endpoint(id = "room-stats")
class RoomStatsEndpoint(val roomSubscriptionService: RoomSubscriptionService) {
    companion object {
        const val MIN_USERS = 5
    }

    @ReadOperation
    fun readStats(): Map<String, Int> {
        return roomSubscriptionService.getUserCountsMap().filter { (_, count) -> count >= MIN_USERS }
    }
}
