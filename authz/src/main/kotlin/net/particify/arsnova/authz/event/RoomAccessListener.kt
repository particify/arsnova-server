package net.particify.arsnova.authz.event

import net.particify.arsnova.authz.handler.RoomAccessHandler
import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.command.SyncRoomAccessCommand
import net.particify.arsnova.authz.model.event.ParticipantAccessMigrationEvent
import net.particify.arsnova.authz.model.event.RoomAccessSyncEvent
import net.particify.arsnova.authz.model.event.RoomCreatedEvent
import net.particify.arsnova.authz.model.event.RoomDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RoomAccessListener(
  private val handler: RoomAccessHandler
) {
  @EventListener
  fun handleRoomCreatedEvent(event: RoomCreatedEvent) {
    handler.create(RoomAccess(event.id, event.ownerId, "1-0", "OWNER", null, null))
  }

  @EventListener
  fun handleRoomDeletedEvent(event: RoomDeletedEvent) {
    handler.deleteByRoomId(event.id)
  }

  @EventListener
  fun handleRoomAccessSyncResponseEvent(event: RoomAccessSyncEvent) {
    handler.handleSyncRoomAccessCommand(SyncRoomAccessCommand(event.rev, event.roomId, event.access))
  }

  @EventListener
  fun handleParticipantAccessMigrationEvent(event: ParticipantAccessMigrationEvent) {
    handler.migrateParticipantAccess(event.userId, event.roomIds)
  }
}
