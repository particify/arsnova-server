package de.thm.arsnova.service.authservice.model.command

class DeleteRoomAccessCommand (
        val rev: String,
        val roomId: String,
        val userId: String
)
