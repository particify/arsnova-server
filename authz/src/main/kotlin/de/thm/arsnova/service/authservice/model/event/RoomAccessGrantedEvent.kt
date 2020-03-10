package de.thm.arsnova.service.authservice.model.event

data class RoomAccessGrantedEvent (
        val version: String = "",
        val roomId: String = "",
        val userId: String = "",
        val role: String = ""
)
