package net.particify.arsnova.authz.event

import net.particify.arsnova.authz.config.RabbitConfig
import net.particify.arsnova.authz.model.event.ParticipantAccessMigrationEvent
import net.particify.arsnova.authz.model.event.RoomAccessSyncEvent
import net.particify.arsnova.authz.model.event.RoomCreatedEvent
import net.particify.arsnova.authz.model.event.RoomDeletedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class AmqpEventDispatcher(
  private val eventPublisher: ApplicationEventPublisher,
) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @RabbitListener(queues = [RabbitConfig.ROOM_CREATED_QUEUE_NAME])
  fun dispatchRoomCreatedEvent(event: RoomCreatedEvent) {
    logger.debug("Dispatching RoomCreatedEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.ROOM_DELETED_QUEUE_NAME])
  fun dispatchRoomDeletedEvent(event: RoomDeletedEvent) {
    logger.debug("Dispatching RoomDeletedEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME])
  fun dispatchRoomAccessSyncEvent(event: RoomAccessSyncEvent) {
    logger.debug("Dispatching RoomAccessSyncEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME])
  fun dispatchParticipantAccessMigrationEvent(event: ParticipantAccessMigrationEvent) {
    logger.debug("Dispatching ParticipantAccessMigrationEvent: {}", event)
    eventPublisher.publishEvent(event)
  }
}
