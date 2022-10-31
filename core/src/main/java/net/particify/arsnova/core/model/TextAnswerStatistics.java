package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class TextAnswerStatistics extends AnswerStatistics {
  public static class TextRoundStatistics extends RoundStatistics {
    private List<String> texts;
    private List<Integer> independentCounts;

    @JsonView(View.Public.class)
    public List<String> getTexts() {
      return texts;
    }

    @JsonView(View.Public.class)
    public void setTexts(final List<String> texts) {
      this.texts = texts;
    }

    @JsonView(View.Public.class)
    public List<Integer> getIndependentCounts() {
      if (independentCounts == null) {
        independentCounts = new ArrayList<>();
      }

      return independentCounts;
    }

    public void setIndependentCounts(final List<Integer> independentCounts) {
      this.independentCounts = independentCounts;
    }
  }

  private List<TextRoundStatistics> roundStatistics;

  @JsonView(View.Public.class)
  public List<TextRoundStatistics> getRoundStatistics() {
    return roundStatistics;
  }

  public void setRoundStatistics(final List<TextRoundStatistics> roundStatistics) {
    this.roundStatistics = roundStatistics;
  }
}
