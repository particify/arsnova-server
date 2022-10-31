package net.particify.arsnova.gateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomHistoryEntry(
  var roomId: String = "",
  var lastVisit: String = ""
)
