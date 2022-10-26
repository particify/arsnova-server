package de.thm.arsnova.service.wsgateway.event

data class RoomUserCountChangedEvent(
        val roomId: String,
        val count: Int,
)
