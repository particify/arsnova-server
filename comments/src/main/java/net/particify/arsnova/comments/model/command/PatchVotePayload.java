package net.particify.arsnova.comments.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.particify.arsnova.comments.model.WebSocketPayload;

public class PatchVotePayload implements WebSocketPayload {
  private UUID id;
  private Map<String, Object> changes;

  public PatchVotePayload() {
  }

  public PatchVotePayload(
      final UUID id,
      final Map<String, Object> changes
  ) {
    this.id = id;
    this.changes = changes;
  }

  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  @JsonProperty("changes")
  public Map<String, Object> getChanges() {
    return changes;
  }

  @JsonProperty("changes")
  public void setChanges(Map<String, Object> changes) {
    this.changes = changes;
  }

  @Override
  public String toString() {
    return "PatchVotePayload{" +
        "id='" + id + '\'' +
        ", changes=" + changes +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PatchVotePayload that = (PatchVotePayload) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(changes, that.changes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, changes);
  }
}
