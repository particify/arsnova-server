package de.thm.arsnova.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import de.thm.arsnova.config.RabbitConfig;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.RoomService;

/**
 * AuthorizationEventDispatcher has the responsibility to send events containing access information to the broker.
 * Worst case scenario: The Dispatcher crashes right after (e.g.) a room got saved to the database, but the
 * corresponding AfterCreationEvent wasn't processed by this dispatcher.
 * In this case, another service depending on that event will have an inconsistent state.
 * ToDo: Check for highest role of moderator and use that string instead of the constants
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
	private static final String CREATOR_ROLE_STRING = "CREATOR";
	private static final String EDITING_MODERATOR_ROLE_STRING = Room.Moderator.Role.EDITING_MODERATOR.name();
	private static final String EXECUTIVE_MODERATOR_ROLE_STRING = Room.Moderator.Role.EXECUTIVE_MODERATOR.name();

	public static final String ROOM_ACCESS_GRANTED_QUEUE_NAME = "backend.event.room.access.granted";
	public static final String ROOM_ACCESS_REVOKED_QUEUE_NAME = "backend.event.room.access.revoked";
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

	@EventListener
	public void handleAfterCreationEventForRoom(final AfterCreationEvent<Room> event) {
		logger.debug("Handling event: {}", event);

		final Room room = event.getEntity();
		final RoomAccessGrantedEvent roomAccessGrantedForCreatorEvent = new RoomAccessGrantedEvent(
				EVENT_VERSION,
				room.getRevision(),
				room.getId(),
				room.getOwnerId(),
				CREATOR_ROLE_STRING
		);

		logger.debug("Sending event: {}, queue: {}", roomAccessGrantedForCreatorEvent, ROOM_ACCESS_GRANTED_QUEUE_NAME);

		messagingTemplate.convertAndSend(
				ROOM_ACCESS_GRANTED_QUEUE_NAME,
				roomAccessGrantedForCreatorEvent
		);

		for (final Room.Moderator removedModerator : room.getModerators()) {
			final RoomAccessGrantedEvent roomAccessGrantedForModeratorEvent = new RoomAccessGrantedEvent(
					EVENT_VERSION,
					room.getRevision(),
					room.getId(),
					removedModerator.getUserId(),
					EXECUTIVE_MODERATOR_ROLE_STRING
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessGrantedForModeratorEvent, ROOM_ACCESS_GRANTED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_GRANTED_QUEUE_NAME,
					roomAccessGrantedForModeratorEvent
			);
		}

		logger.trace("Finished handling event: {}", event);
	}

	@EventListener
	public void handleAfterUpdateEventForRoom(final AfterUpdateEvent<Room> event) {
		logger.debug("Handling event: {}", event);

		final Room oldRoom = event.getOldEntity();
		final Room newRoom = event.getEntity();

		if (!oldRoom.getOwnerId().equals(newRoom.getOwnerId())) {
			final RoomAccessRevokedEvent roomAccessRevokedEvent = new RoomAccessRevokedEvent(
					EVENT_VERSION,
					oldRoom.getRevision(),
					oldRoom.getId(),
					oldRoom.getOwnerId()
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessRevokedEvent, ROOM_ACCESS_REVOKED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_REVOKED_QUEUE_NAME,
					roomAccessRevokedEvent
			);

			final RoomAccessGrantedEvent roomAccessGrantedEvent = new RoomAccessGrantedEvent(
					EVENT_VERSION,
					newRoom.getRevision(),
					newRoom.getId(),
					newRoom.getOwnerId(),
					CREATOR_ROLE_STRING
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessGrantedEvent, ROOM_ACCESS_GRANTED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_GRANTED_QUEUE_NAME,
					roomAccessGrantedEvent
			);
		}

		final Set<Room.Moderator> oldRoomModerators = oldRoom.getModerators();
		final Set<Room.Moderator> newRoomModerators = newRoom.getModerators();

		final Set<Room.Moderator> deletedModerators = getNewMembers(oldRoomModerators, newRoomModerators);

		for (final Room.Moderator removedModerator : deletedModerators) {
			logger.debug("Removed moderator: {}", removedModerator.toString());
			final RoomAccessRevokedEvent roomAccessRevokedEvent = new RoomAccessRevokedEvent(
					EVENT_VERSION,
					oldRoom.getRevision(),
					oldRoom.getId(),
					removedModerator.getUserId()
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessRevokedEvent, ROOM_ACCESS_REVOKED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_REVOKED_QUEUE_NAME,
					roomAccessRevokedEvent
			);
		}

		final Set<Room.Moderator> addedModerators = getNewMembers(newRoomModerators, oldRoomModerators);

		for (final Room.Moderator newModerator : addedModerators) {
			logger.debug("Added moderator: {}", newModerator.toString());
			final RoomAccessGrantedEvent roomAccessGrantedEvent = new RoomAccessGrantedEvent(
					EVENT_VERSION,
					newRoom.getRevision(),
					newRoom.getId(),
					newModerator.getUserId(),
					EXECUTIVE_MODERATOR_ROLE_STRING
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessGrantedEvent, ROOM_ACCESS_GRANTED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_GRANTED_QUEUE_NAME,
					roomAccessGrantedEvent
			);
		}

		logger.trace("Finished handling event: {}", event);
	}

	@EventListener
	public void handleAfterDeletionEventForRoom(final AfterDeletionEvent<Room> event) {
		logger.debug("Handling event: {}", event);

		final Room room = event.getEntity();
		final RoomAccessRevokedEvent roomAccessRevokedEvent = new RoomAccessRevokedEvent(
				EVENT_VERSION,
				room.getRevision(),
				room.getId(),
				room.getOwnerId()
		);

		logger.debug("Sending event: {}, queue: {}", roomAccessRevokedEvent, ROOM_ACCESS_REVOKED_QUEUE_NAME);

		messagingTemplate.convertAndSend(
				ROOM_ACCESS_REVOKED_QUEUE_NAME,
				roomAccessRevokedEvent
		);

		for (final Room.Moderator moderator : room.getModerators()) {
			final RoomAccessRevokedEvent roomAccessRevokedForModeratorEvent = new RoomAccessRevokedEvent(
					EVENT_VERSION,
					room.getRevision(),
					room.getId(),
					moderator.getUserId()
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessRevokedForModeratorEvent, ROOM_ACCESS_REVOKED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_REVOKED_QUEUE_NAME,
					roomAccessRevokedForModeratorEvent
			);
		}

		logger.trace("Finished handling event: {}", event);
	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME)
	@SendTo(ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME)
	public RoomAccessSyncEvent answerRoomAccessSyncRequest(final RoomAccessSyncRequest request) {
		logger.debug("Handling request: {}", request);
		final Room room = roomService.get(request.getRoomId(), true);

		logger.trace("Preparing to send room access sync event for room: {}", room);

		final List<RoomAccessSyncEvent.RoomAccessEntry> accessEntries = new ArrayList<>();

		accessEntries.add(new RoomAccessSyncEvent.RoomAccessEntry(room.getOwnerId(), CREATOR_ROLE_STRING));
		final List<RoomAccessSyncEvent.RoomAccessEntry> modEntries = room.getModerators().stream()
				.map(moderator ->
					new RoomAccessSyncEvent.RoomAccessEntry(
							moderator.getUserId(),
							EXECUTIVE_MODERATOR_ROLE_STRING))
				.collect(Collectors.toList()
		);

		accessEntries.addAll(modEntries);

		final RoomAccessSyncEvent roomAccessSyncEvent = new RoomAccessSyncEvent(
				EVENT_VERSION,
				room.getRevision(),
				room.getId(),
				accessEntries
		);

		logger.debug("Answering with event: {}", roomAccessSyncEvent);

		return roomAccessSyncEvent;
	}

	private Set<Room.Moderator> getNewMembers(final Set<Room.Moderator> check, final Set<Room.Moderator> in) {
		final Set<Room.Moderator> r = check.stream().filter(i -> !in.contains(i)).collect(Collectors.toSet());

		return r;
	}
}
