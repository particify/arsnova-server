package de.thm.arsnova.service.wsgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomAccess (
    var roomId: String? = "",
    var userId: String? = "",
    val rev: String = "",
    var role: String? = ""
)
