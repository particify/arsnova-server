package de.thm.arsnova.service.authservice.model.command

data class RequestRoomAccessSyncCommand(
  val roomId: String,
  val revNumber: Int = 0
)
