package de.thm.arsnova.service.authservice.model

import java.io.Serializable

class RoomAccessPK (
        var roomId: String? = "",
        var userId: String? = ""
) : Serializable {
    override fun toString(): String {
        return "RoomAccessPK(roomId=$roomId, userId=$userId)"
    }
}
