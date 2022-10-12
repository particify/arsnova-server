package de.thm.arsnova.websocket.message;

public class CreateFeedback extends WebSocketMessage<CreateFeedbackPayload> {
	public CreateFeedback() {
		super(CreateFeedback.class.getSimpleName());
	}
}
