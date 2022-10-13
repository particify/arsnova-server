package de.thm.arsnova.service.wsgateway.event

data class RoomJoinEvent(
    val wsSessionId: String,
    val userId: String,
    val roomId: String,
)
