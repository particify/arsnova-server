package net.particify.arsnova.authz.model.event

import net.particify.arsnova.authz.model.RoomAccessEntry

data class RoomAccessSyncEvent(
  val version: String = "",
  val rev: String = "",
  val roomId: String = "",
  val access: List<RoomAccessEntry> = emptyList()
)
