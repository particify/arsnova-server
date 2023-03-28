package net.particify.arsnova.comments.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.config.RabbitConfig;

@Component
public class AmqpEventDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(AmqpEventDispatcher.class);

  private ApplicationEventPublisher eventPublisher;

  public AmqpEventDispatcher(final ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  @RabbitListener(queues = RabbitConfig.ROOM_CREATED_QUEUE_NAME)
  public void dispatchRoomCreatedEvent(final RoomCreatedEvent event) {
    logger.info("Dispatching RoomCreatedEvent: {}", event);
    eventPublisher.publishEvent(event);
  }

  @RabbitListener(queues = RabbitConfig.ROOM_DELETED_QUEUE_NAME)
  public void dispatchRoomDeletedEvent(final RoomDeletedEvent event) {
    logger.info("Dispatching RoomDeletedEvent: {}", event);
    eventPublisher.publishEvent(event);
  }

  @RabbitListener(queues = RabbitConfig.BACKEND_COMMENT_QUEUE_NAME)
  public void dispatchImportEvent(final ImportEvent event) {
    logger.info("Dispatching ImportEvent: {}", event);
    eventPublisher.publishEvent(event);
  }

  @RabbitListener(queues = RabbitConfig.BACKEND_ROOM_DUPLICATED_QUEUE_NAME)
  @Transactional
  public void dispatchRoomDuplicatedEvent(final RoomDuplicatedEvent event) {
    logger.info("Dispatching RoomDuplicatedEvent: {}", event);
    eventPublisher.publishEvent(event);
  }
}
