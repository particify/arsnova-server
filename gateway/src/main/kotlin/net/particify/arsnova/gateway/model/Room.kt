package net.particify.arsnova.gateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Room(
  var id: String,
  var shortId: String,
  var name: String,
)
