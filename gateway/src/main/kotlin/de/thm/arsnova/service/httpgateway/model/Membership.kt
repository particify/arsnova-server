package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Membership (
    var roomId: String = "",
    var roomShortId: String = "",
    var roles: Set<String> = setOf(),
    var lastVisit: String
)
