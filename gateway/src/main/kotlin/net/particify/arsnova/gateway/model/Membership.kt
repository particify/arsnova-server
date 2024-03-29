package net.particify.arsnova.gateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class Membership(
  var roomId: String = "",
  var roomShortId: String = "",
  var roles: Set<String> = setOf(),
  var lastVisit: Date,
)
