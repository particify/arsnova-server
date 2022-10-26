package de.thm.arsnova.service.wsgateway.event

data class RoomLeaveEvent(
        val wsSessionId: String,
        val userId: String,
        val roomId: String,
)
