package net.particify.arsnova.websocket.event

data class RoomUserCountChangedEvent(
  val roomId: String,
  val count: Int,
)
