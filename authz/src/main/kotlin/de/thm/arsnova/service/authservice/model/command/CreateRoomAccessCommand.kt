package de.thm.arsnova.service.authservice.model.command

data class CreateRoomAccessCommand (
        val roomId: String,
        val userId: String,
        val role: String
)
