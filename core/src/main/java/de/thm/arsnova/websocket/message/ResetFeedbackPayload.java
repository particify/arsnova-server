package de.thm.arsnova.websocket.message;

public class ResetFeedbackPayload implements WebSocketPayload {
	String roomId;

	public ResetFeedbackPayload() {
	}

	public ResetFeedbackPayload(final String roomId) {
		this.roomId = roomId;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}
}
