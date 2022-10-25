package net.particify.arsnova.authz.persistence

import net.particify.arsnova.authz.model.RoomAccessSyncTracker
import org.springframework.data.repository.CrudRepository

interface RoomAccessSyncTrackerRepository : CrudRepository<RoomAccessSyncTracker, String>
