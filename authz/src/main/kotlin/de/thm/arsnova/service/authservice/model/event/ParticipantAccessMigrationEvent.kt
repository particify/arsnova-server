package de.thm.arsnova.service.authservice.model.event

data class ParticipantAccessMigrationEvent(
  val userId: String = "",
  val roomIds: List<String> = emptyList()
)
