package de.thm.arsnova.service.authservice.persistence

import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessPK
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoomAccessRepository : CrudRepository<RoomAccess, RoomAccessPK> {
    fun findByRoomId(roomId: String): Iterable<RoomAccess>
    fun findByUserId(userId: String): Iterable<RoomAccess>
}
