package net.particify.arsnova.authz.model.command

import java.util.UUID

data class RequestRoomAccessSyncCommand(
  val roomId: UUID,
  val revNumber: Int = 0
)
