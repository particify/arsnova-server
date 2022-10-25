package net.particify.arsnova.authz.model.event

data class ParticipantAccessMigrationEvent(
  val userId: String = "",
  val roomIds: List<String> = emptyList()
)
