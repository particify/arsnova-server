package net.particify.arsnova.gateway.model

data class AccessChangeRequest(
  val type: AccessChangeRequestType,
  val roomId: String,
  val revId: String,
  val userId: String,
  val level: AccessLevel
)
