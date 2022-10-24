package de.thm.arsnova.websocket.message;

public class CreateFeedbackPayload implements WebSocketPayload {
  String roomId;
  String userId;
  int value;

  public CreateFeedbackPayload() {
  }

  public CreateFeedbackPayload(final String roomId, final String userId, final int value) {
    this.roomId = roomId;
    this.userId = userId;
    this.value = value;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(final String roomId) {
    this.roomId = roomId;
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
