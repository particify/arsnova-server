package net.particify.arsnova.comments.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

import net.particify.arsnova.comments.model.WebSocketMessage;
import net.particify.arsnova.comments.model.WebSocketPayload;

public class WebSocketEvent<P extends WebSocketPayload> extends WebSocketMessage<P> {
  // roomId of the entity the event is based on
  protected String roomId;

  public WebSocketEvent(String type) {
    super(type);
  }

  public WebSocketEvent(String type, String roomId) {
    super(type);
    this.roomId = roomId;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final WebSocketEvent<?> that = (WebSocketEvent<?>) o;

    return Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), roomId);
  }

  @Override
  public String toString() {
    return "WebSocketEvent{" +
        "type='" + type + '\'' +
        ", roomId='" + roomId+ '\'' +
        ", payload=" + payload.toString() +
        '}';
  }
}
