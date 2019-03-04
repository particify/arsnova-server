package de.thm.arsnova.websocket.handler;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.GetFeedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class FeedbackHandler {
	private final FeedbackCommandHandler commandHandler;

	@Autowired
	public FeedbackHandler(FeedbackCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@MessageMapping("/room/{roomId}/feedback.command")
	public void send(
			@DestinationVariable("roomId") String roomId,
			CreateFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.CreateFeedbackCommand(roomId, value)
		);

	}

	@MessageMapping("/room/{roomId}/feedback.query")
	public void send(
			@DestinationVariable("roomId") String roomId,
			GetFeedback value
	) throws Exception {

		commandHandler.handle(
				new FeedbackCommandHandler.GetFeedbackCommand(roomId, value)
		);

	}

}
