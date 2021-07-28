package de.thm.arsnova.service.httpgateway.model

class User(
    var id: String = "",
    var roomHistory: List<RoomHistoryEntry> = listOf()
)
