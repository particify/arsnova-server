package net.particify.arsnova.authz.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date
import java.util.UUID

@Entity
@IdClass(RoomAccessPK::class)
class RoomAccess(
  @Id
  var roomId: UUID,
  @Id
  var userId: UUID,
  val rev: String = "",
  var role: String? = "",
  @Temporal(TemporalType.TIMESTAMP)
  var creationTimestamp: Date? = null,
  @Temporal(TemporalType.TIMESTAMP)
  var lastAccess: Date? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RoomAccess

    if (roomId != other.roomId) return false
    if (userId != other.userId) return false
    if (rev != other.rev) return false
    if (role != other.role) return false
    if (creationTimestamp != other.creationTimestamp) return false
    if (lastAccess != other.lastAccess) return false

    return true
  }

  override fun hashCode(): Int {
    var result = roomId.hashCode()
    result = 31 * result + userId.hashCode()
    result = 31 * result + rev.hashCode()
    result = 31 * result + (role?.hashCode() ?: 0)
    result = 31 * result + creationTimestamp.hashCode()
    result = 31 * result + lastAccess.hashCode()
    return result
  }

  override fun toString(): String =
    """
    RoomAccess(
      roomId=$roomId,
      userId=$userId,
      rev='$rev',
      role=$role,
      creationTimestamp=$creationTimestamp,
      lastAccess=$lastAccess
    )
    """.trimIndent()
}
