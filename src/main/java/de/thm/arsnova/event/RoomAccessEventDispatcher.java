package de.thm.arsnova.event;

import java.util.Set;
import java.util.stream.Collectors;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import de.thm.arsnova.model.Room;

/**
 * AuthorizationEventDispatcher has the responsibility to send events containing access information to the broker.
 * Worst case scenario: The Dispatcher crashes right after (e.g.) a room got saved to the database, but the
 * corresponding AfterCreationEvent wasn't processed by this dispatcher.
 * In this case, another service depending on that event will have an inconsistent state.
 * ToDo: Check for highest role of moderator and use that string instead of the constants
 *
 * @author Tom Käsler
 */
public class RoomAccessEventDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(RoomAccessEventDispatcher.class);

	private static final String EVENT_VERSION = "1";
	private static final String CREATOR_ROLE_STRING = "CREATOR";
	private static final String EDITING_MODERATOR_ROLE_STRING = Room.Moderator.Role.EDITING_MODERATOR.name();
	private static final String EXECUTIVE_MODERATOR_ROLE_STRING = Room.Moderator.Role.EXECUTIVE_MODERATOR.name();

	private static final String ROOM_ACCESS_GRANTED_QUEUE_NAME = "backend.event.room.access.granted";
	private static final String ROOM_ACCESS_REVOKED_QUEUE_NAME = "backend.event.room.access.revoked";

	private final RabbitTemplate messagingTemplate;

	@Autowired
	public RoomAccessEventDispatcher(
			final RabbitTemplate rabbitTemplate
	) {
		messagingTemplate = rabbitTemplate;
	}

	@EventListener
	public void handleAfterCreationEventForRoom(final AfterCreationEvent<Room> event) {
		logger.debug("Handling event: {}", event);

		final Room room = event.getEntity();
		final RoomAccessGrantedEvent roomAccessGrantedForCreatorEvent = new RoomAccessGrantedEvent(
				EVENT_VERSION,
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

		final Set<Room.Moderator> deletedModerators = getNewMembers(newRoomModerators, oldRoomModerators);

		for (final Room.Moderator removedModerator : deletedModerators) {
			final RoomAccessRevokedEvent roomAccessRevokedEvent = new RoomAccessRevokedEvent(
					EVENT_VERSION,
					oldRoom.getId(),
					removedModerator.getUserId()
			);

			logger.debug("Sending event: {}, queue: {}", roomAccessRevokedEvent, ROOM_ACCESS_REVOKED_QUEUE_NAME);

			messagingTemplate.convertAndSend(
					ROOM_ACCESS_REVOKED_QUEUE_NAME,
					roomAccessRevokedEvent
			);
		}

		final Set<Room.Moderator> addedModerators = getNewMembers(oldRoomModerators, newRoomModerators);

		for (final Room.Moderator newModerator : addedModerators) {
			final RoomAccessGrantedEvent roomAccessGrantedEvent = new RoomAccessGrantedEvent(
					EVENT_VERSION,
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

	private Set<Room.Moderator> getNewMembers(final Set<Room.Moderator> a, final Set<Room.Moderator> b) {
		final Set<Room.Moderator> r = a.stream().filter(i -> !b.contains(i)).collect(Collectors.toSet());

		return r;
	}
}
