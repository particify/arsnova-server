package net.particify.arsnova.authz.persistence

import net.particify.arsnova.authz.model.RoomAccessSyncTracker
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface RoomAccessSyncTrackerRepository : CrudRepository<RoomAccessSyncTracker, UUID>
