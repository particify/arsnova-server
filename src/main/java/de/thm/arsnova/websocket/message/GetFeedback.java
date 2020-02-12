package de.thm.arsnova.websocket.message;

public class GetFeedback extends WebSocketMessage<GetFeedbackPayload> {
	public GetFeedback() {
		super(GetFeedback.class.getSimpleName());
	}
}

