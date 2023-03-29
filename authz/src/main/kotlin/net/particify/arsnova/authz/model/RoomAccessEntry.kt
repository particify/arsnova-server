package net.particify.arsnova.authz.model

import java.util.UUID

data class RoomAccessEntry(
  val userId: UUID,
  val role: String = ""
)
