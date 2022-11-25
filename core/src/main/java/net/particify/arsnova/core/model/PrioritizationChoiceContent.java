package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

public class PrioritizationChoiceContent extends ChoiceQuestionContent {
  private int assignablePoints = 100;

  public PrioritizationChoiceContent() {

  }

  public PrioritizationChoiceContent(final PrioritizationChoiceContent content) {
    super(content);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getAssignablePoints() {
    return assignablePoints;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAssignablePoints(final int assignablePoints) {
    this.assignablePoints = assignablePoints;
  }
}
