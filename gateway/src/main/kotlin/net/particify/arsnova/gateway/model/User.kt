package net.particify.arsnova.gateway.model

import java.util.Date

class User(
  var id: String = "",
  var roomHistory: List<RoomHistoryEntry> = listOf(),
  var announcementReadTimestamp: Date?,
)
