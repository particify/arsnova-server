package de.thm.arsnova.websocket.message;

public class GetFeedbackPayload implements WebSocketPayload {
	String roomId;

	public GetFeedbackPayload() {
	}

	public GetFeedbackPayload(final String roomId) {
		this.roomId = roomId;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}
}
