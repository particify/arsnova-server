package de.thm.arsnova.service.comment.model.command;

import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class DeleteCommentPayload implements WebSocketPayload {
  private String id;

  public DeleteCommentPayload() {
  }

  public DeleteCommentPayload(String id) {
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
