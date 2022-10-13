package de.thm.arsnova.service.comment.model.event;

import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class CommentDeletedPayload implements WebSocketPayload {
  private String id;

  public CommentDeletedPayload() {
  }

  public CommentDeletedPayload(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
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
