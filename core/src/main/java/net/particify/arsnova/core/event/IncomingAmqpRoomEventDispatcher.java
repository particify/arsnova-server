package net.particify.arsnova.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import net.particify.arsnova.common.uuid.UuidHelper;
import net.particify.arsnova.core.config.RabbitConfig;
import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Room;

@Component
@ConditionalOnProperty(
    name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
    prefix = MessageBrokerProperties.PREFIX,
    havingValue = "true")
@ConditionalOnProperty(
    name = "external-room-management",
    prefix = SystemProperties.PREFIX,
    havingValue = "true")
public class IncomingAmqpRoomEventDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(IncomingAmqpRoomEventDispatcher.class);

  private final ApplicationEventPublisher eventPublisher;

  public IncomingAmqpRoomEventDispatcher(final ApplicationEventPublisher applicationEventPublisher) {
    logger.debug("Using IncomingAmqpEventDispatcher due to external room management.");
    this.eventPublisher = applicationEventPublisher;
  }

  @RabbitListener(queues = RabbitConfig.ROOM_AFTER_DELETION_QUEUE_NAME)
  public void dispatchAfterRoomDeletionEvent(final AmqpRoomDeletionEvent event) {
    logger.debug("Dispatching AmqpRoomDeletionEvent: {}", event);
    final Room room = new Room();
    room.setId(UuidHelper.uuidToString(event.id()));
    eventPublisher.publishEvent(new BeforeDeletionEvent<>(this, room));
    eventPublisher.publishEvent(new AfterDeletionEvent<>(this, room));
  }

  @RabbitListener(queues = RabbitConfig.ROOM_DUPLICATION_QUEUE_NAME)
  public void dispatchRoomDuplicationEvent(final AmqpRoomDuplicationEvent event) {
    logger.debug("Dispatching AmqpRoomDuplicationEvent: {}", event);
    final Room originalRoom = new Room();
    originalRoom.setId(UuidHelper.uuidToString(event.originalRoomId()));
    final Room duplicatedRoom = new Room();
    duplicatedRoom.setId(UuidHelper.uuidToString(event.duplicatedRoomId()));
    eventPublisher.publishEvent(new RoomDuplicationEvent(this, originalRoom, duplicatedRoom));
  }
}
