package net.particify.arsnova.gateway.model

data class SummarizedStats(
  val connectedUsers: Int,
  val users: Int,
  val activationsPending: Int,
  val rooms: Int,
  val contents: Int,
  val answers: Int,
  val comments: Int
)
