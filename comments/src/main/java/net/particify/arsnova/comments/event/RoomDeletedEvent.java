package net.particify.arsnova.comments.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
class RoomDeletedEvent {
  private UUID id;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "RoomDeletedEvent{" +
      "id='" + id + '\'' +
      '}';
  }
}
