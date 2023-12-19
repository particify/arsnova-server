package net.particify.arsnova.authz.model

import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class RoomAccessPK(
  var roomId: UUID,
  var userId: UUID,
) : Serializable
