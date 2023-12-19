package net.particify.arsnova.websocket.event

data class RoomLeaveEvent(
  val wsSessionId: String,
  val userId: String,
  val roomId: String,
)
