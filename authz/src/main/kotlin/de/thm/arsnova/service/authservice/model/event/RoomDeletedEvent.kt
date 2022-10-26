package de.thm.arsnova.service.authservice.model.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomDeletedEvent (
        val id: String = "",
        val ownerId: String = ""
)
