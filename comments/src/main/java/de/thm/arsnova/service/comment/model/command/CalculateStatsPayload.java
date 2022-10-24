package de.thm.arsnova.service.comment.model.command;

import java.util.List;
import java.util.Objects;

import de.thm.arsnova.service.comment.model.WebSocketPayload;

public class CalculateStatsPayload implements WebSocketPayload {
  private List<String> roomIds;

  public CalculateStatsPayload() {
  }

  public CalculateStatsPayload(final List<String> roomIds) {
    this.roomIds = roomIds;
  }

  public List<String> getRoomIds() {
    return roomIds;
  }

  public void setRoomIds(final List<String> roomIds) {
    this.roomIds = roomIds;
  }

  @Override
  public String toString() {
    return "CalculateStatsPayload{" +
        "roomIds=" + roomIds +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CalculateStatsPayload that = (CalculateStatsPayload) o;
    return Objects.equals(roomIds, that.roomIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomIds);
  }
}
