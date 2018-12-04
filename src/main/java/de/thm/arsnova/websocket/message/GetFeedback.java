package de.thm.arsnova.websocket.message;

public class GetFeedback extends WebSocketMessage<WebSocketPayload> {
	public GetFeedback() {
		super(GetFeedback.class.getSimpleName());
	}
}

