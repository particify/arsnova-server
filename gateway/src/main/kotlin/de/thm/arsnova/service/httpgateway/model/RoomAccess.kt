package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomAccess(
    var roomId: String = "",
    var userId: String = "",
    val rev: String = "",
    var role: String = "",
    var lastAccess: Date?
)
