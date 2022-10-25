package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class ChoiceAnswerStatistics extends AnswerStatistics {

  public static class ChoiceRoundStatistics extends RoundStatistics {
    public static class Combination {
      private List<Integer> selectedChoiceIndexes;
      private int count;

      public Combination(final List<Integer> selectedChoiceIndexes, final int count) {
        this.selectedChoiceIndexes = selectedChoiceIndexes;
        this.count = count;
      }

      @JsonView(View.Public.class)
      public List<Integer> getSelectedChoiceIndexes() {
        return selectedChoiceIndexes;
      }

      @JsonView(View.Public.class)
      public int getCount() {
        return count;
      }

      @Override
      public String toString() {
        return new ToStringCreator(this)
            .append("selectedChoiceIndexes", selectedChoiceIndexes)
            .append("count", count)
            .toString();
      }
    }

    private List<Integer> independentCounts;
    private Collection<Combination> combinatedCounts;

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

    @JsonView(View.Public.class)
    public Collection<Combination> getCombinatedCounts() {
      if (combinatedCounts == null) {
        combinatedCounts = new ArrayList<>();
      }

      return combinatedCounts;
    }

    public void setCombinatedCounts(final Collection<Combination> combinatedCounts) {
      this.combinatedCounts = combinatedCounts;
    }

    @Override
    public String toString() {
      return new ToStringCreator(super.toString())
          .append("independentCounts", independentCounts)
          .append("combinatedCounts", combinatedCounts)
          .toString();
    }
  }

  public static class RoundTransition {
    private int roundA;
    private int roundB;
    private List<Integer> selectedChoiceIndexesA;
    private List<Integer> selectedChoiceIndexesB;
    private int count;

    public RoundTransition(final int roundA, final List<Integer> selectedChoiceIndexesA,
        final int roundB, final List<Integer> selectedChoiceIndexesB, final int count) {
      this.roundA = roundA;
      this.roundB = roundB;
      this.selectedChoiceIndexesA = selectedChoiceIndexesA;
      this.selectedChoiceIndexesB = selectedChoiceIndexesB;
      this.count = count;
    }

    @JsonView(View.Public.class)
    public int getRoundA() {
      return roundA;
    }

    @JsonView(View.Public.class)
    public int getRoundB() {
      return roundB;
    }

    @JsonView(View.Public.class)
    public List<Integer> getSelectedChoiceIndexesA() {
      return selectedChoiceIndexesA;
    }

    @JsonView(View.Public.class)
    public List<Integer> getSelectedChoiceIndexesB() {
      return selectedChoiceIndexesB;
    }

    @JsonView(View.Public.class)
    public int getCount() {
      return count;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("roundA", roundA)
          .append("selectedChoiceIndexesA", selectedChoiceIndexesA)
          .append("roundB", roundB)
          .append("selectedChoiceIndexesB", selectedChoiceIndexesB)
          .append("count", count)
          .toString();
    }
  }

  private List<ChoiceRoundStatistics> roundStatistics;
  private List<RoundTransition> roundTransitions;

  @JsonView(View.Public.class)
  public List<ChoiceRoundStatistics> getRoundStatistics() {
    return roundStatistics;
  }

  public void setRoundStatistics(final List<ChoiceRoundStatistics> roundStatistics) {
    this.roundStatistics = roundStatistics;
  }

  @JsonView(View.Public.class)
  public List<RoundTransition> getRoundTransitions() {
    return roundTransitions;
  }

  public void setRoundTransitions(final List<RoundTransition> roundTransitions) {
    this.roundTransitions = roundTransitions;
  }

  @Override
  public String toString() {
    return new ToStringCreator(super.toString())
        .append("roundStatistics", roundStatistics)
        .append("roundTransitions", roundTransitions)
        .toString();
  }

}
