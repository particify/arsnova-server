package de.thm.arsnova.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public class RoomAccessSyncRequest {
  private String roomId;

  public RoomAccessSyncRequest(
      @JsonProperty("roomId") final String roomId) {
    this.roomId = roomId;
  }

  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(final String roomId) {
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
    final RoomAccessSyncRequest that = (RoomAccessSyncRequest) o;
    return Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId);
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
        .append("roomId", roomId)
        .toString();
  }
}
