package net.particify.arsnova.authz.model.event

import net.particify.arsnova.authz.model.RoomAccessEntry
import java.util.UUID

data class RoomAccessSyncEvent(
  val version: String = "",
  val rev: String = "",
  val roomId: UUID,
  val access: List<RoomAccessEntry> = emptyList()
)
