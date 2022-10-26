package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomFeatures(
    val roomId: String,
    val features: List<String>,
    val tierId: String?
)
