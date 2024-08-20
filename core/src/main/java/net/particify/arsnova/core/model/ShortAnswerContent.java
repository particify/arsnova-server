package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.particify.arsnova.core.model.serialization.View;

public class ShortAnswerContent extends WordContent {
  private Set<String> correctTerms = new HashSet<>();

  public ShortAnswerContent() {

  }

  public ShortAnswerContent(final ShortAnswerContent content) {
    super(content);
    this.correctTerms = content.correctTerms;
  }

  @JsonView({View.Persistence.class, View.Extended.class})
  public Set<String> getCorrectTerms() {
    return correctTerms;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCorrectTerms(final Set<String> correctTerms) {
    this.correctTerms = correctTerms;
  }

  @JsonView(View.Public.class)
  public boolean isScorable() {
    return true;
  }

  @Override
  public List<String> getCorrectnessCriteria() {
    return new ArrayList<>(correctTerms);
  }

  @Override
  public AnswerResult determineAnswerResult(final Answer answer) {
    if (answer instanceof ShortAnswer shortAnswer) {
      return determineAnswerResult(shortAnswer);
    }
    return super.determineAnswerResult(answer);
  }

  public AnswerResult determineAnswerResult(final ShortAnswer answer) {
    if (answer.isAbstention()) {
      return new AnswerResult(
        this.id,
        0,
        0,
        this.getPoints(),
        0,
        AnswerResult.AnswerResultState.ABSTAINED);
    }

    final double achievedPoints = calculateAchievedPoints(answer.getText());
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
    if (answer instanceof ShortAnswer shortAnswer && shortAnswer.getText() != null) {
      return calculateAchievedPoints(shortAnswer.getText());
    }
    return super.calculateAchievedPoints(answer);
  }

  private double calculateAchievedPoints(final String text) {
    return correctTerms.stream().anyMatch(ca -> normalizeText(ca).equals(normalizeText(text)))
        ? getPoints() : 0;
  }

  @Override
  public ShortAnswerContent copy() {
    return new ShortAnswerContent(this);
  }

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
    final ShortAnswerContent that = (ShortAnswerContent) o;
    return Objects.equals(correctTerms, that.correctTerms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), correctTerms);
  }
}
