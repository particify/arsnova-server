package net.particify.arsnova.authz.model.event

import java.util.UUID

data class ParticipantAccessMigrationEvent(
  val userId: UUID,
  val roomIds: List<UUID> = emptyList(),
)
