package de.thm.arsnova.service.authservice.model.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomCreatedEvent(
    val id: String = "",
    val ownerId: String = ""
)
