package de.thm.arsnova.websocket;

import de.thm.arsnova.controller.handler.FeedbackCommandHandler;
import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.CreateFeedbackPayload;
import de.thm.arsnova.websocket.message.FeedbackChanged;
import de.thm.arsnova.websocket.message.FeedbackChangedPayload;
import de.thm.arsnova.websocket.message.GetFeedback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class FeedbackCommandHandlerTest {

	@MockBean
	private SimpMessagingTemplate messagingTemplate;

	private FeedbackCommandHandler commandHandler;

	@Before
	public void setUp() {
		this.commandHandler = new FeedbackCommandHandler(messagingTemplate);
	}

	@Test
	public void getFeedback() {
		String roomId = "12345678";
		GetFeedback getFeedback = new GetFeedback();
		FeedbackCommandHandler.GetFeedbackCommand getFeedbackCommand =
				new FeedbackCommandHandler.GetFeedbackCommand(roomId, null);

		commandHandler.handle(getFeedbackCommand);

		FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		int[] expectedVals = new int[]{0, 0, 0, 0};
		feedbackChangedPayload.setValues(expectedVals);
		FeedbackChanged feedbackChanged = new FeedbackChanged();
		feedbackChanged.setPayload(feedbackChangedPayload);

		ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FeedbackChanged> messageCaptor =
				ArgumentCaptor.forClass(FeedbackChanged.class);

		verify(messagingTemplate).convertAndSend(topicCaptor.capture(), messageCaptor.capture());
		assertThat(topicCaptor.getValue()).isEqualTo("/room/" + roomId + "/feedback");
		assertThat(messageCaptor.getValue()).isEqualTo(feedbackChanged);
	}

	@Test
	public void sendFeedback() {
		String roomId = "12345678";
		CreateFeedbackPayload createFeedbackPayload = new CreateFeedbackPayload(1);
		createFeedbackPayload.setValue(1);
		CreateFeedback createFeedback = new CreateFeedback();
		createFeedback.setPayload(createFeedbackPayload);
		FeedbackCommandHandler.CreateFeedbackCommand createFeedbackCommand =
				new FeedbackCommandHandler.CreateFeedbackCommand(roomId, createFeedback);

		commandHandler.handle(createFeedbackCommand);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(messagingTemplate).convertAndSend(captor.capture(), any(FeedbackChanged.class));
		assertThat(captor.getValue()).isEqualTo("/room/" + roomId + "/feedback");
	}
}


