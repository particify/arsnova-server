package net.particify.arsnova.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.service.RoomService;

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

  @MockitoBean
  private RabbitTemplate messagingTemplate;

  @MockitoBean
  private RoomService roomService;

  private RoomAccessEventDispatcher roomAccessEventDispatcher;

  private Room getTestRoom() {
    final Room room = new Room();
    room.setName(SOME_TEXT);
    room.setId(SOME_ROOM_ID);
    room.setRevision(SOME_ROOM_REV);
    room.setShortId(SOME_ROOM_SHORT_ID);
    room.setOwnerId(TEST_USER_ID);
    return room;
  }

  @BeforeEach
  public void setUp() {
    this.roomAccessEventDispatcher = new RoomAccessEventDispatcher(messagingTemplate, roomService);
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
        Arrays.asList(new RoomAccessSyncEvent.RoomAccessEntry(testRoom.getOwnerId(), "OWNER"))
    );

    final RoomAccessSyncEvent response = roomAccessEventDispatcher.answerRoomAccessSyncRequest(request);

    assertThat(response).isEqualTo(expectedResponse);
  }
}
