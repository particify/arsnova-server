package de.thm.arsnova.websocket.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import de.thm.arsnova.websocket.message.CreateFeedback;

@RunWith(SpringRunner.class)
public class FeedbackHandlerTest {

	@MockBean
	private FeedbackCommandHandler feedbackCommandHandler;

	private FeedbackHandler feedbackHandler;

	@Before
	public void setUp() {
		this.feedbackHandler = new FeedbackHandler(feedbackCommandHandler);
	}

	@Test
	public void sendFeedback() throws Exception {
		feedbackHandler.send(
				"12345678",
				new CreateFeedback()
		);

		final ArgumentCaptor<FeedbackCommandHandler.CreateFeedbackCommand> captor =
				ArgumentCaptor.forClass(FeedbackCommandHandler.CreateFeedbackCommand.class);
		verify(feedbackCommandHandler, times(1)).handle(captor.capture());

		assertThat(captor.getValue().getRoomId()).isEqualTo("12345678");
		assertThat(captor.getValue().getPayload()).isInstanceOf(CreateFeedback.class);
	}
}
