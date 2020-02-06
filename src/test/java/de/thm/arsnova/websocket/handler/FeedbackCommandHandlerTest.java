package de.thm.arsnova.websocket.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.FeedbackStorageService;
import de.thm.arsnova.service.RoomService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.CreateFeedbackPayload;
import de.thm.arsnova.websocket.message.FeedbackChanged;
import de.thm.arsnova.websocket.message.FeedbackChangedPayload;
import de.thm.arsnova.websocket.message.GetFeedback;

@RunWith(SpringRunner.class)
public class FeedbackCommandHandlerTest {

	@MockBean
	private RabbitTemplate messagingTemplate;

	@MockBean
	private FeedbackStorageService feedbackStorage;

	@MockBean
	private RoomService roomService;

	private FeedbackCommandHandler commandHandler;

	private Room getTestRoom() {
		Room r = new Room();
		r.setId("12345678");
		r.getSettings().setFeedbackLocked(false);
		return r;
	}

	@Before
	public void setUp() {
		this.commandHandler = new FeedbackCommandHandler(messagingTemplate, feedbackStorage, roomService);
	}

	@Test
	public void getFeedback() {
		Room r = getTestRoom();
		final String roomId = r.getId();

		Mockito.when(roomService.get(roomId, true)).thenReturn(r);
		Mockito.when(feedbackStorage.getByRoom(r)).thenReturn(new Feedback(0, 0, 0, 0));

		final GetFeedback getFeedback = new GetFeedback();

		commandHandler.handle(getFeedback);

		final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		final int[] expectedVals = new int[]{0, 0, 0, 0};
		feedbackChangedPayload.setValues(expectedVals);
		final FeedbackChanged feedbackChanged = new FeedbackChanged();
		feedbackChanged.setPayload(feedbackChangedPayload);

		final ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<FeedbackChanged> messageCaptor =
				ArgumentCaptor.forClass(FeedbackChanged.class);

		verify(messagingTemplate).convertAndSend(topicCaptor.capture(), messageCaptor.capture());
		assertThat(topicCaptor.getValue()).isEqualTo("/topic/" + roomId + ".feedback.stream");
		assertThat(messageCaptor.getValue()).isEqualTo(feedbackChanged);
	}

	@Test
	public void sendFeedback() {
		Room r = getTestRoom();
		final String roomId = r.getId();

		Mockito.when(roomService.get(roomId, true)).thenReturn(r);
		Mockito.when(feedbackStorage.getByRoom(r)).thenReturn(new Feedback(0, 1, 0, 0));

		final CreateFeedbackPayload createFeedbackPayload = new CreateFeedbackPayload(roomId, "1", 1);
		createFeedbackPayload.setValue(1);
		final CreateFeedback createFeedback = new CreateFeedback();
		createFeedback.setPayload(createFeedbackPayload);

		commandHandler.handle(createFeedback);

		final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(messagingTemplate).convertAndSend(captor.capture(), any(FeedbackChanged.class));
		assertThat(captor.getValue()).isEqualTo("/topic/" + roomId + ".feedback.stream");
	}
}


