package net.particify.arsnova.authz.model.event

import java.util.UUID

data class RoomAccessSyncRequest(
  val roomId: UUID,
)
