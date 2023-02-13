package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class PrioritizationChoiceContent extends ChoiceQuestionContent {
  private int assignablePoints = 100;

  public PrioritizationChoiceContent() {

  }

  public PrioritizationChoiceContent(final PrioritizationChoiceContent content) {
    super(content);
    this.assignablePoints = content.assignablePoints;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getAssignablePoints() {
    return assignablePoints;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAssignablePoints(final int assignablePoints) {
    this.assignablePoints = assignablePoints;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>PriorizationChoiceContent</tt> are included in equality checks.
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
    final PrioritizationChoiceContent that = (PrioritizationChoiceContent) o;

    return assignablePoints == that.assignablePoints;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), assignablePoints);
  }
}
