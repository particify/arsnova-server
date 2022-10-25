package net.particify.arsnova.authz.model

import java.io.Serializable

data class RoomAccessPK(
  var roomId: String? = "",
  var userId: String? = ""
) : Serializable
