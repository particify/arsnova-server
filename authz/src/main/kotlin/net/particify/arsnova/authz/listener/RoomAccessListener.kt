package net.particify.arsnova.authz.listener

import net.particify.arsnova.authz.config.RabbitConfig
import net.particify.arsnova.authz.handler.RoomAccessHandler
import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.command.SyncRoomAccessCommand
import net.particify.arsnova.authz.model.event.ParticipantAccessMigrationEvent
import net.particify.arsnova.authz.model.event.RoomAccessSyncEvent
import net.particify.arsnova.authz.model.event.RoomCreatedEvent
import net.particify.arsnova.authz.model.event.RoomDeletedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class RoomAccessListener(
  private val handler: RoomAccessHandler
) {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  @RabbitListener(queues = [RabbitConfig.roomCreatedQueueName])
  fun receiveRoomCreatedEvent(event: RoomCreatedEvent) {
    logger.debug("Got event on room created queue: {}", event)
    handler.create(RoomAccess(event.id, event.ownerId, "1-0", "CREATOR", null, null))
  }

  @RabbitListener(queues = [RabbitConfig.roomDeletedQueueName])
  fun receiveRoomDeletedEvent(event: RoomDeletedEvent) {
    logger.debug("Got event on room deleted queue: {}", event)
    handler.deleteByRoomId(event.id)
  }

  @RabbitListener(queues = [RabbitConfig.roomAccessSyncResponseQueueName])
  fun receiveRoomAccessSyncResponseEvent(event: RoomAccessSyncEvent) {
    logger.debug("Got event on room access sync response queue: {}", event)
    handler.handleSyncRoomAccessCommand(SyncRoomAccessCommand(event.rev, event.roomId, event.access))
  }

  @RabbitListener(queues = [RabbitConfig.participantAccessMigrationQueueName])
  fun receiveParticipantAccessMigrationEvent(event: ParticipantAccessMigrationEvent) {
    logger.debug("Got event on participant access migration queue: {}", event)
    handler.migrateParticipantAccess(event.userId, event.roomIds)
  }
}
