package de.thm.arsnova.websocket.message;

public class ResetFeedback extends WebSocketMessage<ResetFeedbackPayload> {
	public ResetFeedback() {
		super(GetFeedback.class.getSimpleName());
	}
}
