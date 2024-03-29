package net.particify.arsnova.gateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomSubscription(
  var roomId: String = "",
  var tier: String = "",
  var tierId: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserSubscription(
  var userId: String = "",
  var tier: String = "",
  var tierId: String = "",
)
