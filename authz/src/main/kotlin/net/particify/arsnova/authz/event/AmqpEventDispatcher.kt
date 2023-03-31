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
class AmqpEventDispatcher(private val eventPublisher: ApplicationEventPublisher) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @RabbitListener(queues = [RabbitConfig.roomCreatedQueueName])
  fun dispatchRoomCreatedEvent(event: RoomCreatedEvent) {
    logger.debug("Dispatching RoomCreatedEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.roomDeletedQueueName])
  fun dispatchRoomDeletedEvent(event: RoomDeletedEvent) {
    logger.debug("Dispatching RoomDeletedEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.roomAccessSyncResponseQueueName])
  fun dispatchRoomAccessSyncEvent(event: RoomAccessSyncEvent) {
    logger.debug("Dispatching RoomAccessSyncEvent: {}", event)
    eventPublisher.publishEvent(event)
  }

  @RabbitListener(queues = [RabbitConfig.participantAccessMigrationQueueName])
  fun dispatchParticipantAccessMigrationEvent(event: ParticipantAccessMigrationEvent) {
    logger.debug("Dispatching ParticipantAccessMigrationEvent: {}", event)
    eventPublisher.publishEvent(event)
  }
}
