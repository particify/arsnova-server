package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class PrioritizationAnswerStatistics extends AnswerStatistics {

  public static class PrioritizationRoundStatistics extends RoundStatistics {
    private List<Integer> assignedPoints;

    @JsonView(View.Public.class)
    public List<Integer> getAssignedPoints() {
      return assignedPoints;
    }

    @JsonView(View.Public.class)
    public void setAssignedPoints(final List<Integer> assignedPoints) {
      this.assignedPoints = assignedPoints;
    }
  }

  private List<PrioritizationRoundStatistics> roundStatistics;

  @JsonView(View.Public.class)
  public List<PrioritizationRoundStatistics> getRoundStatistics() {
    return roundStatistics;
  }

  public void setRoundStatistics(final List<PrioritizationRoundStatistics> roundStatistics) {
    this.roundStatistics = roundStatistics;
  }
}
