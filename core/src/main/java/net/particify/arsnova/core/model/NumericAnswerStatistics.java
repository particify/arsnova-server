package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class NumericAnswerStatistics extends AnswerStatistics {
  public static class NumericRoundStatistics extends RoundStatistics {
    private List<Double> selectedNumbers;
    private List<Integer> independentCounts;
    private double mean;
    private double median;
    private double standardDeviation;
    private double variance;
    private double minimum;
    private double maximum;
    private Double correctAnswerFraction;

    @JsonView(View.Public.class)
    public List<Double> getSelectedNumbers() {
      if (selectedNumbers == null) {
        selectedNumbers = new ArrayList<>();
      }
      return selectedNumbers;
    }

    @JsonView(View.Public.class)
    public void setSelectedNumbers(final List<Double> selectedNumbers) {
      this.selectedNumbers = selectedNumbers;
    }

    @JsonView(View.Public.class)
    public List<Integer> getIndependentCounts() {
      if (independentCounts == null) {
        independentCounts = new ArrayList<>();
      }
      return independentCounts;
    }

    @JsonView(View.Public.class)
    public void setIndependentCounts(final List<Integer> independentCounts) {
      this.independentCounts = independentCounts;
    }

    @JsonView(View.Public.class)
    public double getMean() {
      return mean;
    }

    @JsonView(View.Public.class)
    public void setMean(final double mean) {
      this.mean = mean;
    }

    @JsonView(View.Public.class)
    public double getMedian() {
      return median;
    }

    @JsonView(View.Public.class)
    public void setMedian(final double median) {
      this.median = median;
    }

    @JsonView(View.Public.class)
    public double getStandardDeviation() {
      return standardDeviation;
    }

    @JsonView(View.Public.class)
    public void setStandardDeviation(final double standardDeviation) {
      this.standardDeviation = standardDeviation;
    }

    @JsonView(View.Public.class)
    public double getVariance() {
      return variance;
    }

    @JsonView(View.Public.class)
    public void setVariance(final double variance) {
      this.variance = variance;
    }

    @JsonView(View.Public.class)
    public double getMinimum() {
      return minimum;
    }

    @JsonView(View.Public.class)
    public void setMinimum(final double minimum) {
      this.minimum = minimum;
    }

    @JsonView(View.Public.class)
    public double getMaximum() {
      return maximum;
    }

    @JsonView(View.Public.class)
    public void setMaximum(final double maximum) {
      this.maximum = maximum;
    }

    @JsonView(View.Public.class)
    public Double getCorrectAnswerFraction() {
      return correctAnswerFraction;
    }

    @JsonView(View.Public.class)
    public void setCorrectAnswerFraction(final Double correctAnswerFraction) {
      this.correctAnswerFraction = correctAnswerFraction;
    }
  }

  private List<NumericRoundStatistics> roundStatistics;

  @JsonView(View.Public.class)
  public List<NumericRoundStatistics> getRoundStatistics() {
    return roundStatistics;
  }

  public void setRoundStatistics(final List<NumericRoundStatistics> roundStatistics) {
    this.roundStatistics = roundStatistics;
  }
}
