package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class PriorizationAnswer extends Answer {
  private List<@NotNull @PositiveOrZero Integer> assignedPoints = new ArrayList<>();

  public PriorizationAnswer() {

  }

  public PriorizationAnswer(final PriorizationChoiceContent content, final String creatorId) {
    super(content, creatorId);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<Integer> getAssignedPoints() {
    return assignedPoints;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAssignedPoints(final List<Integer> assignedPoints) {
    this.assignedPoints = assignedPoints;
  }

  @Override
  public boolean isAbstention() {
    return assignedPoints.isEmpty();
  }
}
