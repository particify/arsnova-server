package de.thm.arsnova.websocket.message;

public class CreateFeedbackPayload implements WebSocketPayload {
	int value;

	public CreateFeedbackPayload() {
	}

	public CreateFeedbackPayload(final int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(final int value) {
		this.value = value;
	}
}
