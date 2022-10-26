package de.thm.arsnova.websocket.message;

public class FeedbackStopped extends WebSocketMessage<WebSocketPayload> {
	public FeedbackStopped() {
		super(FeedbackStopped.class.getSimpleName());
	}
}
