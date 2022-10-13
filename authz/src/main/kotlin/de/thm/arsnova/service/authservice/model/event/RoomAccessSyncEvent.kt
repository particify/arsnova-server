package de.thm.arsnova.service.authservice.model.event

import de.thm.arsnova.service.authservice.model.RoomAccessEntry

data class RoomAccessSyncEvent(
  val version: String = "",
  val rev: String = "",
  val roomId: String = "",
  val access: List<RoomAccessEntry> = emptyList()
)
