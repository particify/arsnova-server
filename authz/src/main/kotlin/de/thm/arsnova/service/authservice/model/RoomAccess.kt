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
        var role: String? = ""
)
