package de.thm.arsnova.websocket.message;

public class StopFeedback extends WebSocketMessage<WebSocketPayload> {
	public StopFeedback() {
		super(StopFeedback.class.getSimpleName());
	}
}

