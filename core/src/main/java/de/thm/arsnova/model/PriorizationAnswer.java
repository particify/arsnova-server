package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import de.thm.arsnova.model.serialization.View;

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
