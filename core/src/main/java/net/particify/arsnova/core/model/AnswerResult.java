package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public class AnswerResult {
  private String contentId;
  private double achievedPoints;

  private double competitivePoints;
  private int maxPoints;
  private int duration;
  private AnswerResultState state;

  public AnswerResult(
      final String contentId,
      final double achievedPoints,
      final double competitivePoints,
      final int maxPoints,
      final int duration,
      final AnswerResultState state) {
    this.contentId = contentId;
    this.achievedPoints = achievedPoints;
    this.competitivePoints = competitivePoints;
    this.maxPoints = maxPoints;
    this.duration = duration;
    this.state = state;
  }

  public String getContentId() {
    return contentId;
  }

  public double getAchievedPoints() {
    return achievedPoints;
  }

  public double getCompetitivePoints() {
    return competitivePoints;
  }

  public void setCompetitivePoints(final double competitivePoints) {
    this.competitivePoints = competitivePoints;
  }

  public int getMaxPoints() {
    return maxPoints;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(final int duration) {
    this.duration = duration;
  }

  public AnswerResultState getState() {
    return state;
  }

  public void setState(final AnswerResultState state) {
    this.state = state;
  }

  public enum AnswerResultState {
    UNANSWERED,
    ABSTAINED,
    CORRECT,
    WRONG,
    NEUTRAL
  }
}
