package net.particify.arsnova.authz.model.command

class DeleteRoomAccessCommand(
  val rev: String,
  val roomId: String,
  val userId: String
)
