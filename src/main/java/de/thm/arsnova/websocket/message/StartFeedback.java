package de.thm.arsnova.websocket.message;

public class StartFeedback extends WebSocketMessage<WebSocketPayload> {
	public StartFeedback() {
		super(StartFeedback.class.getSimpleName());
	}
}

