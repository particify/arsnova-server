package net.particify.arsnova.authz.model.command

import net.particify.arsnova.authz.model.RoomAccessEntry

class SyncRoomAccessCommand(
  val rev: String = "",
  val roomId: String = "",
  val access: List<RoomAccessEntry> = emptyList()
)
