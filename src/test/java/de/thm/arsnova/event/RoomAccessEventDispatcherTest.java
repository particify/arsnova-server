package de.thm.arsnova.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.RoomService;

@ExtendWith(SpringExtension.class)
public class RoomAccessEventDispatcherTest {
	private static final String TEST_USER_ID = "TestUser";
	private static final String SOME_TEXT = "SomeText";
	private static final String SOME_ROOM_SHORT_ID = "87878787";
	private static final String SOME_ROOM_ID = "2da635c77f114dc48ff9179731a7782e";
	private static final String SOME_ROOM_REV = "1-2da635c77f114dc48ff9179731a7782e";
	private static final String SOME_MODERATOR_ID_1 = "eea635c77f114dc48ff9179731a778ee";
	private static final String SOME_MODERATOR_ID_2 = "ffa635c77f114dc48ff9179731a778ff";

	private static final String ROOM_ACCESS_GRANTED_QUEUE_NAME = "backend.event.room.access.granted";
	private static final String ROOM_ACCESS_REVOKED_QUEUE_NAME = "backend.event.room.access.revoked";

	@MockBean
	private RabbitTemplate messagingTemplate;

	@MockBean
	private RoomService roomService;

	private RoomAccessEventDispatcher roomAccessEventDispatcher;

	private Room getTestRoom() {
		final Room room = new Room();
		room.setName(SOME_TEXT);
		room.setAbbreviation(SOME_TEXT);
		room.setId(SOME_ROOM_ID);
		room.setRevision(SOME_ROOM_REV);
		room.setShortId(SOME_ROOM_SHORT_ID);
		room.setOwnerId(TEST_USER_ID);
		return room;
	}

	private Set<Room.Moderator> getAModerator() {
		final Set<Room.Moderator> moderators = new HashSet<>();
		final Room.Moderator aModerator = new Room.Moderator();
		final Set<Room.Moderator.Role> roles = new HashSet<>();
		roles.add(Room.Moderator.Role.EXECUTIVE_MODERATOR);
		aModerator.setRoles(roles);
		aModerator.setUserId(SOME_MODERATOR_ID_1);
		moderators.add(aModerator);
		return moderators;
	}

	private Set<Room.Moderator> getAnotherModerator() {
		final Set<Room.Moderator> moderators = new HashSet<>();
		final Room.Moderator aModerator = new Room.Moderator();
		final Set<Room.Moderator.Role> roles = new HashSet<>();
		roles.add(Room.Moderator.Role.EXECUTIVE_MODERATOR);
		aModerator.setRoles(roles);
		aModerator.setUserId(SOME_MODERATOR_ID_2);
		moderators.add(aModerator);
		return moderators;
	}

	@BeforeEach
	public void setUp() {
		this.roomAccessEventDispatcher = new RoomAccessEventDispatcher(messagingTemplate, roomService);
	}

	@Test
	public void testNewRoomAccess() {
		final Room room = getTestRoom();

		final AfterCreationEvent<Room> afterCreationEvent = new AfterCreationEvent<>(this, room);
		roomAccessEventDispatcher.handleAfterCreationEventForRoom(afterCreationEvent);

		final ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<RoomAccessGrantedEvent> messageCaptor =
			ArgumentCaptor.forClass(RoomAccessGrantedEvent.class);

		final RoomAccessGrantedEvent expectedEvent = new RoomAccessGrantedEvent(
			"1",
			SOME_ROOM_REV,
			SOME_ROOM_ID,
			TEST_USER_ID,
			"CREATOR"
		);

		verify(messagingTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture());
		assertThat(queueCaptor.getValue()).isEqualTo(ROOM_ACCESS_GRANTED_QUEUE_NAME);
		assertThat(messageCaptor.getValue()).isEqualTo(expectedEvent);
	}

	@Test
	public void testChangedModeratorRoomAccess() {
		final Room oldState = getTestRoom();
		final Room newState = getTestRoom();

		oldState.setModerators(getAModerator());
		newState.setModerators(getAnotherModerator());

		final AfterFullUpdateEvent<Room> afterFullUpdateEvent = new AfterFullUpdateEvent<>(
			this,
			newState,
			oldState
		);

		roomAccessEventDispatcher.handleAfterUpdateEventForRoom(afterFullUpdateEvent);

		final ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<RoomAccessEvent> messageCaptor =
			ArgumentCaptor.forClass(RoomAccessEvent.class);

		verify(messagingTemplate, times(2))
				.convertAndSend(queueCaptor.capture(), messageCaptor.capture());

		List<String> queues = queueCaptor.getAllValues();
		List<RoomAccessEvent> events = messageCaptor.getAllValues();

		assertThat(queues.get(0)).isEqualTo(ROOM_ACCESS_REVOKED_QUEUE_NAME);
		assertThat(queues.get(1)).isEqualTo(ROOM_ACCESS_GRANTED_QUEUE_NAME);

		assertThat(events.get(0)).isInstanceOf(RoomAccessRevokedEvent.class);
		assertThat(events.get(1)).isInstanceOf(RoomAccessGrantedEvent.class);
	}

	@Test
	public void testDeleteRoomAccess() {
		final Room room = getTestRoom();
		room.setModerators(getAModerator());

		final AfterDeletionEvent<Room> afterDeletionEvent = new AfterDeletionEvent<>(this, room);
		roomAccessEventDispatcher.handleAfterDeletionEventForRoom(afterDeletionEvent);

		final RoomAccessRevokedEvent roomAccessRevokedForOwnerEvent = new RoomAccessRevokedEvent(
				"1",
				SOME_ROOM_REV,
				SOME_ROOM_ID,
				TEST_USER_ID
		);

		final RoomAccessRevokedEvent roomAccessRevokedForModeratorEvent = new RoomAccessRevokedEvent(
				"1",
				SOME_ROOM_REV,
				SOME_ROOM_ID,
				SOME_MODERATOR_ID_1
		);


		final ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<RoomAccessRevokedEvent> messageCaptor =
				ArgumentCaptor.forClass(RoomAccessRevokedEvent.class);

		verify(messagingTemplate, times(2))
				.convertAndSend(queueCaptor.capture(), messageCaptor.capture());

		List<String> queues = queueCaptor.getAllValues();
		List<RoomAccessRevokedEvent> events = messageCaptor.getAllValues();

		assertThat(queues.get(0)).isEqualTo(ROOM_ACCESS_REVOKED_QUEUE_NAME);
		assertThat(queues.get(1)).isEqualTo(ROOM_ACCESS_REVOKED_QUEUE_NAME);

		assertThat(events.get(0)).isEqualTo(roomAccessRevokedForOwnerEvent);
		assertThat(events.get(1)).isEqualTo(roomAccessRevokedForModeratorEvent);
	}

	@Test
	public void testRespondToSyncRequest() {
		final Room testRoom = getTestRoom();
		final RoomAccessSyncRequest request = new RoomAccessSyncRequest(testRoom.getId());

		Mockito.when(roomService.get(testRoom.getId(), true)).thenReturn(testRoom);

		final RoomAccessSyncEvent expectedResponse = new RoomAccessSyncEvent(
				"1",
				testRoom.getRevision(),
				testRoom.getId(),
				Arrays.asList(new RoomAccessSyncEvent.RoomAccessEntry(testRoom.getOwnerId(), "CREATOR"))
		);

		final RoomAccessSyncEvent response = roomAccessEventDispatcher.answerRoomAccessSyncRequest(request);

		assertThat(response).isEqualTo(expectedResponse);
	}
}
