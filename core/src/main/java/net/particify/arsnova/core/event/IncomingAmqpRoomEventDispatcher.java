package net.particify.arsnova.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.RabbitConfig;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Room;

@Component
@ConditionalOnProperty(
    name = "external-room-management",
    prefix = SystemProperties.PREFIX,
    havingValue = "true")
public class IncomingAmqpRoomEventDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(IncomingAmqpRoomEventDispatcher.class);

  private ApplicationEventPublisher eventPublisher;

  public IncomingAmqpRoomEventDispatcher(final ApplicationEventPublisher applicationEventPublisher) {
    logger.debug("Using IncomingAmqpEventDispatcher due to external room management.");
    this.eventPublisher = applicationEventPublisher;
  }

  @RabbitListener(queues = RabbitConfig.ROOM_BEFORE_DELETION_QUEUE_NAME)
  public void dispatchBeforeRoomDeletionEvent(final BeforeDeletionEvent<Room> event) {
    logger.debug("Dispatching BeforeDeletionEvent: {}", event);
    eventPublisher.publishEvent(event);
  }

  @RabbitListener(queues = RabbitConfig.ROOM_AFTER_DELETION_QUEUE_NAME)
  public void dispatchAfterRoomDeletionEvent(final AfterDeletionEvent<Room> event) {
    logger.debug("Dispatching AfterDeletionEvent: {}", event);
    eventPublisher.publishEvent(event);
  }

  @RabbitListener(queues = RabbitConfig.ROOM_DUPLICATION_QUEUE_NAME)
  public void dispatchRoomDuplicationEvent(final RoomDuplicationEvent event) {
    logger.debug("Dispatching RoomDuplicationEvent: {}", event);
    eventPublisher.publishEvent(event);
  }
}
