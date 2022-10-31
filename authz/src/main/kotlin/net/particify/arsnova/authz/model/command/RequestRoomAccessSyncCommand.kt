package net.particify.arsnova.authz.model.command

data class RequestRoomAccessSyncCommand(
  val roomId: String,
  val revNumber: Int = 0
)
