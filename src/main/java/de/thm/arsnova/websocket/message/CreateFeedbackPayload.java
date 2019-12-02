package de.thm.arsnova.websocket.message;

public class CreateFeedbackPayload implements WebSocketPayload {
	String userId;
	int value;

	public CreateFeedbackPayload() {
	}

	public CreateFeedbackPayload(final String userId, final int value) {
		this.userId = userId;
		this.value = value;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(final String userId) {
		this.userId = userId;
	}

	public int getValue() {
		return value;
	}

	public void setValue(final int value) {
		this.value = value;
	}
}
