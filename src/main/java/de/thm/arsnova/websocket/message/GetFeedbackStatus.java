package de.thm.arsnova.websocket.message;

public class GetFeedbackStatus extends WebSocketMessage<WebSocketPayload> {
	public GetFeedbackStatus() {
		super(GetFeedback.class.getSimpleName());
	}
}

