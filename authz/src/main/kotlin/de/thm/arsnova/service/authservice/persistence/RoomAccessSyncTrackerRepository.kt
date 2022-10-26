package de.thm.arsnova.service.authservice.persistence

import de.thm.arsnova.service.authservice.model.RoomAccessSyncTracker
import org.springframework.data.repository.CrudRepository

interface RoomAccessSyncTrackerRepository : CrudRepository<RoomAccessSyncTracker, String> {

}