package net.particify.arsnova.authz.model.command

import net.particify.arsnova.authz.model.RoomAccessEntry
import java.util.UUID

class SyncRoomAccessCommand(
  val rev: String = "",
  val roomId: UUID,
  val access: List<RoomAccessEntry> = emptyList(),
)
