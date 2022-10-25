package net.particify.arsnova.websocket.event

data class RoomJoinEvent(
  val wsSessionId: String,
  val userId: String,
  val roomId: String,
)
