package de.thm.arsnova.service.authservice.model.command

import de.thm.arsnova.service.authservice.model.RoomAccessEntry

class SyncRoomAccessCommand (
    val rev: String = "",
    val roomId: String = "",
    val access: List<RoomAccessEntry> = emptyList()
)
