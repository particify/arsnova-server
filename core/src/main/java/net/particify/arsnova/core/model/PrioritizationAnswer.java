package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class PrioritizationAnswer extends Answer {
  private List<@NotNull @PositiveOrZero Integer> assignedPoints = new ArrayList<>();

  public PrioritizationAnswer() {

  }

  public PrioritizationAnswer(final PrioritizationChoiceContent content, final String creatorId) {
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

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>PriorizationAnswer</tt> are included in equality checks.
   * </p>
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final PrioritizationAnswer that = (PrioritizationAnswer) o;

    return Objects.equals(assignedPoints, that.assignedPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), assignedPoints);
  }
}
