package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomSubscription (
        var roomId: String = "",
        var tier: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserSubscription (
        var userId: String = "",
        var tier: String = ""
)
