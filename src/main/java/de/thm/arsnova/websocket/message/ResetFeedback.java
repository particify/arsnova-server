package de.thm.arsnova.websocket.message;

public class ResetFeedback extends WebSocketMessage<WebSocketPayload> {
	public ResetFeedback() {
		super(GetFeedback.class.getSimpleName());
	}
}
