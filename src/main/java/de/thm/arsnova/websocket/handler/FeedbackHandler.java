package de.thm.arsnova.websocket.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.GetFeedback;
import de.thm.arsnova.websocket.message.GetFeedbackStatus;
import de.thm.arsnova.websocket.message.StartFeedback;
import de.thm.arsnova.websocket.message.StopFeedback;

@Controller
public class FeedbackHandler {
	private final FeedbackCommandHandler commandHandler;

	@Autowired
	public FeedbackHandler(final FeedbackCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@MessageMapping("/queue/{roomId}.feedback.command")
	public void send(
			@DestinationVariable("roomId") final String roomId,
			final CreateFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.CreateFeedbackCommand(roomId, value)
		);

	}

	@MessageMapping("/queue/{roomId}.feedback.command.status")
	public void send(
			@DestinationVariable("roomId") final String roomId,
			final GetFeedbackStatus value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.GetFeedbackStatusCommand(roomId, value)
		);

	}

	@MessageMapping("/queue/{roomId}.feedback.command.start")
	public void send(
			@DestinationVariable("roomId") final String roomId,
			final StartFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.StartFeedbackCommand(roomId, value)
		);

	}

	@MessageMapping("/queue/{roomId}.feedback.command.stop")
	public void send(
			@DestinationVariable("roomId") final String roomId,
			final StopFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.StopFeedbackCommand(roomId, value)
		);

	}

	@MessageMapping("/queue/{roomId}.feedback.query")
	public void send(
			@DestinationVariable("roomId") final String roomId,
			final GetFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.GetFeedbackCommand(roomId, value)
		);

	}

}
