package de.thm.arsnova.websocket.message;

public class FeedbackReset extends WebSocketMessage<WebSocketPayload> {
	public FeedbackReset() {
		super(FeedbackReset.class.getSimpleName());
	}
}
