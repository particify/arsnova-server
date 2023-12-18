package net.particify.arsnova.authz.model.command

import java.util.UUID

class DeleteRoomAccessCommand(
  val rev: String,
  val roomId: UUID,
  val userId: UUID,
)
