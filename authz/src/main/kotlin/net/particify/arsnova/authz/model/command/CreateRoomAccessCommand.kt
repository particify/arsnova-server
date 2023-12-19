package net.particify.arsnova.authz.model.command

import java.util.UUID

data class CreateRoomAccessCommand(
  val rev: String,
  val roomId: UUID,
  val userId: UUID,
  val role: String,
)
