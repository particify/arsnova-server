package net.particify.arsnova.comments.model.command;
import java.util.Objects;
import java.util.UUID;

import net.particify.arsnova.comments.model.WebSocketPayload;

public class DeleteCommentsByRoomPayload implements WebSocketPayload {
  private UUID roomId;

  public DeleteCommentsByRoomPayload() {
  }

  public DeleteCommentsByRoomPayload(UUID roomId) {
    this.roomId = roomId;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }

  @Override
  public String toString() {
    return "DeleteCommentPayload{" +
        "id='" + roomId + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeleteCommentsByRoomPayload payload = (DeleteCommentsByRoomPayload) o;
    return Objects.equals(roomId, payload.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId);
  }
}
