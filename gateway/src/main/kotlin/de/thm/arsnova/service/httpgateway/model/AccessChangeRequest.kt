package de.thm.arsnova.service.httpgateway.model

data class AccessChangeRequest(
  val type: AccessChangeRequestType,
  val roomId: String,
  val revId: String,
  val userId: String,
  val level: AccessLevel
)
