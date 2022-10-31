package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public abstract class RoundStatistics {
  private int round;
  private int answerCount;
  private int abstentionCount;

  @JsonView(View.Public.class)
  public int getRound() {
    return round;
  }

  public void setRound(final int round) {
    this.round = round;
  }

  @JsonView(View.Public.class)
  public int getAnswerCount() {
    return answerCount;
  }

  public void setAnswerCount(final int answerCount) {
    this.answerCount = answerCount;
  }

  @JsonView(View.Public.class)
  public int getAbstentionCount() {
    return abstentionCount;
  }

  public void setAbstentionCount(final int abstentionCount) {
    this.abstentionCount = abstentionCount;
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
      .append("round", round)
      .append("answerCount", answerCount)
      .append("abstentionCount", abstentionCount)
      .toString();
  }
}
