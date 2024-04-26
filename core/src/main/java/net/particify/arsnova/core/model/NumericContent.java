package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class NumericContent extends Content {
  private double minNumber;
  private double maxNumber;
  private Double correctNumber;
  private double tolerance = 0.0;

  public NumericContent() {

  }

  public NumericContent(final NumericContent content) {
    super(content);
    this.minNumber = content.minNumber;
    this.maxNumber = content.maxNumber;
    this.correctNumber = content.correctNumber;
    this.tolerance = content.tolerance;

  }

  @JsonView({View.Persistence.class, View.Public.class})
  public double getMinNumber() {
    return minNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setMinNumber(final double minNumber) {
    this.minNumber = minNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public double getMaxNumber() {
    return maxNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setMaxNumber(final double maxNumber) {
    this.maxNumber = maxNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Double getCorrectNumber() {
    return correctNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCorrectNumber(final Double correctNumber) {
    this.correctNumber = correctNumber;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public double getTolerance() {
    return tolerance;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTolerance(final double tolerance) {
    this.tolerance = tolerance;
  }

  @JsonView(View.Public.class)
  public int getPoints() {
    return isScorable() ? 10 : 0;
  }

  @JsonView(View.Public.class)
  public boolean isScorable() {
    return correctNumber != null;
  }

  @Override
  public AnswerResult determineAnswerResult(final Answer answer) {
    if (answer instanceof NumericAnswer numericAnswer) {
      return determineAnswerResult(numericAnswer);
    }

    return super.determineAnswerResult(answer);
  }

  public AnswerResult determineAnswerResult(final NumericAnswer answer) {
    if (answer.isAbstention()) {
      return new AnswerResult(
          this.id,
          0,
          0,
          this.getPoints(),
          0,
          AnswerResult.AnswerResultState.ABSTAINED);
    }

    if (!isScorable()) {
      return new AnswerResult(
          this.id,
          0,
          0,
          this.getPoints(),
          0,
          AnswerResult.AnswerResultState.NEUTRAL);
    }

    final double achievedPoints = calculateAchievedPoints(answer.getSelectedNumber());
    final AnswerResult.AnswerResultState state = achievedPoints == getPoints()
        ? AnswerResult.AnswerResultState.CORRECT : AnswerResult.AnswerResultState.WRONG;

    return new AnswerResult(
        this.id,
        achievedPoints,
        calculateCompetitivePoints(answer.getCreationTimestamp().toInstant(), achievedPoints),
        this.getPoints(),
        answer.getDurationMs(),
        state);
  }

  @Override
  public double calculateAchievedPoints(final Answer answer) {
    if (answer instanceof NumericAnswer numericAnswer) {
      return calculateAchievedPoints(numericAnswer.getSelectedNumber());
    }
    return super.calculateAchievedPoints(answer);
  }

  private double calculateAchievedPoints(final double selectedNumber) {
    return selectedNumber >= correctNumber - tolerance && selectedNumber <= correctNumber + tolerance
        ? getPoints() : 0;
  }

  @Override
  public NumericContent copy() {
    return new NumericContent(this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>NumericContent</tt> are included in equality checks.
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
    final NumericContent that = (NumericContent) o;

    return minNumber == that.minNumber && maxNumber == that.maxNumber
        && Objects.equals(correctNumber, that.correctNumber) && tolerance == that.tolerance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), minNumber, maxNumber, correctNumber, tolerance);
  }
}
