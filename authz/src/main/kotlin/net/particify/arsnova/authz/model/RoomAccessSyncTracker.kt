package net.particify.arsnova.authz.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class RoomAccessSyncTracker(
  @Id
  var roomId: String = "",
  var rev: String = ""
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RoomAccessSyncTracker

    if (roomId != other.roomId) return false
    if (rev != other.rev) return false

    return true
  }

  override fun hashCode(): Int {
    var result = roomId.hashCode()
    result = 31 * result + rev.hashCode()
    return result
  }

  override fun toString(): String {
    return "RoomAccessSyncTracker(roomId='$roomId', rev='$rev')"
  }
}
