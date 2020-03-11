package de.thm.arsnova.service.authservice.model

import java.io.Serializable

data class RoomAccessPK (
        var roomId: String? = "",
        var userId: String? = ""
) : Serializable
