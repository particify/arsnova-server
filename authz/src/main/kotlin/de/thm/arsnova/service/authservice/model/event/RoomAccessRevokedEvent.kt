package de.thm.arsnova.service.authservice.model.event

data class RoomAccessRevokedEvent (
        val version: String = "",
        val rev: String = "",
        val roomId: String = "",
        val userId: String = ""
)
