package net.particify.arsnova.core.event;

import java.util.ArrayList;
import java.util.List;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.RabbitConfig;
import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.service.RoomService;

/**
 * AuthorizationEventDispatcher has the responsibility to send events containing access information to the broker.
 * Worst case scenario: The Dispatcher crashes right after (e.g.) a room got saved to the database, but the
 * corresponding AfterCreationEvent wasn't processed by this dispatcher.
 * In this case, another service depending on that event will have an inconsistent state.
 *
 * @author Tom Käsler
 */
@Component
@ConditionalOnProperty(
    name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
    prefix = MessageBrokerProperties.PREFIX,
    havingValue = "true")
public class RoomAccessEventDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(RoomAccessEventDispatcher.class);

  private static final String EVENT_VERSION = "1";
  private static final String OWNER_ROLE_STRING = "OWNER";

  public static final String ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME = "backend.event.room.access.sync.request";
  public static final String ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME = "backend.event.room.access.sync.response";

  private final RabbitTemplate messagingTemplate;
  private final RoomService roomService;

  @Autowired
  public RoomAccessEventDispatcher(
      final RabbitTemplate rabbitTemplate,
      final RoomService roomService
  ) {
    messagingTemplate = rabbitTemplate;
    this.roomService = roomService;
  }

  @RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME)
  @SendTo(ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME)
  public RoomAccessSyncEvent answerRoomAccessSyncRequest(final RoomAccessSyncRequest request) {
    logger.debug("Handling request: {}", request);
    try {
      final Room room = roomService.get(request.getRoomId(), true);

      logger.trace("Preparing to send room access sync event for room: {}", room);

      final List<RoomAccessSyncEvent.RoomAccessEntry> accessEntries = new ArrayList<>();

      accessEntries.add(new RoomAccessSyncEvent.RoomAccessEntry(room.getOwnerId(), OWNER_ROLE_STRING));

      final RoomAccessSyncEvent roomAccessSyncEvent = new RoomAccessSyncEvent(
          EVENT_VERSION,
          room.getRevision(),
          room.getId(),
          accessEntries
      );

      logger.debug("Answering with event: {}", roomAccessSyncEvent);

      return roomAccessSyncEvent;
    } catch (final DocumentNotFoundException e) {
      logger.warn("Got sync request for non-existing room: {}", request);

      return null;
    }
  }
}
