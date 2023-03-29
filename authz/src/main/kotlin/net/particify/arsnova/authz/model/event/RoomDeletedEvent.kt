package net.particify.arsnova.authz.model.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomDeletedEvent(
  val id: UUID,
  val ownerId: UUID
)
