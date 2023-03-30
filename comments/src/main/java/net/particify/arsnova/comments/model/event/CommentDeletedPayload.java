package net.particify.arsnova.comments.model.event;

import java.util.Objects;
import java.util.UUID;

import net.particify.arsnova.comments.model.WebSocketPayload;

public class CommentDeletedPayload implements WebSocketPayload {
  private UUID id;

  public CommentDeletedPayload() {
  }

  public CommentDeletedPayload(UUID id) {
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
    return "CommentDeletedPayload{" +
        "id='" + id + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommentDeletedPayload that = (CommentDeletedPayload) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
