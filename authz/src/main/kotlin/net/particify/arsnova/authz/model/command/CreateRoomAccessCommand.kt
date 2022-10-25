package net.particify.arsnova.authz.model.command

data class CreateRoomAccessCommand(
  val rev: String,
  val roomId: String,
  val userId: String,
  val role: String
)
