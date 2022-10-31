package net.particify.arsnova.authz.model.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomCreatedEvent(
  val id: String = "",
  val ownerId: String = ""
)
