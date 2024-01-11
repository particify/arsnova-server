package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class NumericAnswer extends Answer {
  private Double selectedNumber;

  public NumericAnswer() {
  }

  public NumericAnswer(final NumericContent content, final String creatorId) {
    super(content, creatorId);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Double getSelectedNumber() {
    return selectedNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setSelectedNumber(final double selectedNumber) {
    this.selectedNumber = selectedNumber;
  }

  @Override
  public boolean isAbstention() {
    return selectedNumber == null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>NumericAnswer</tt> are included in equality checks.
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
    final NumericAnswer that = (NumericAnswer) o;

    return Objects.equals(selectedNumber, that.selectedNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), selectedNumber);
  }
}
