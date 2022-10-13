package de.thm.arsnova.service.comment.model.command;

import java.io.Serializable;

public class CalculateStats extends WebSocketCommand<CalculateStatsPayload> implements Serializable {
  public CalculateStats() {
    super(CalculateStats.class.getSimpleName());
  }

  public CalculateStats(CalculateStatsPayload p) {
    super(CalculateStats.class.getSimpleName());
    this.payload = p;
  }
}
