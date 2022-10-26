package de.thm.arsnova.service.authservice.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass

@Entity
@IdClass(RoomAccessPK::class)
class RoomAccess (
        @Id
        var roomId: String? = "",
        @Id
        var userId: String? = "",
        val rev: String = "",
        var role: String? = ""
) {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as RoomAccess

                if (roomId != other.roomId) return false
                if (userId != other.userId) return false
                if (rev != other.rev) return false
                if (role != other.role) return false

                return true
        }

        override fun hashCode(): Int {
                var result = roomId?.hashCode() ?: 0
                result = 31 * result + (userId?.hashCode() ?: 0)
                result = 31 * result + rev.hashCode()
                result = 31 * result + (role?.hashCode() ?: 0)
                return result
        }

        override fun toString(): String {
                return "RoomAccess(roomId=$roomId, userId=$userId, rev='$rev', role=$role)"
        }
}
