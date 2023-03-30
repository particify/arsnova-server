package net.particify.arsnova.comments.model.command;

import java.util.Objects;
import java.util.UUID;

import net.particify.arsnova.comments.model.WebSocketPayload;

public class DeleteCommentPayload implements WebSocketPayload {
  private UUID id;

  public DeleteCommentPayload() {
  }

  public DeleteCommentPayload(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "DeleteCommentPayload{" +
        "id='" + id + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeleteCommentPayload payload = (DeleteCommentPayload) o;
    return Objects.equals(id, payload.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
