package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public class AnswerStatisticsUserSummary {
  private int correctAnswerCount;
  private int scorableContentCount;
  private double achievedScore;
  private int maxScore;
  private List<AnswerResult> answerResults;

  public AnswerStatisticsUserSummary(
      final int correctAnswerCount,
      final int scorableContentCount,
      final double achievedScore,
      final int maxScore,
      final List<AnswerResult> answerResults) {
    this.correctAnswerCount = correctAnswerCount;
    this.scorableContentCount = scorableContentCount;
    this.achievedScore = achievedScore;
    this.maxScore = maxScore;
    this.answerResults = answerResults;
  }

  public int getCorrectAnswerCount() {
    return correctAnswerCount;
  }

  public int getScorableContentCount() {
    return scorableContentCount;
  }

  public double getAchievedScore() {
    return achievedScore;
  }

  public int getMaxScore() {
    return maxScore;
  }

  public List<AnswerResult> getAnswerResults() {
    return answerResults;
  }
}
