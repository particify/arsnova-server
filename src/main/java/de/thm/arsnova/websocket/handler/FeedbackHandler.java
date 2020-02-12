package de.thm.arsnova.websocket.handler;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.GetFeedback;

@Service
public class FeedbackHandler {
	private final FeedbackCommandHandler commandHandler;

	@Autowired
	public FeedbackHandler(final FeedbackCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "feedback.command")
	public void receiveMessage(
			final CreateFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "feedback.query")
	public void receiveMessage(
			final GetFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

}
