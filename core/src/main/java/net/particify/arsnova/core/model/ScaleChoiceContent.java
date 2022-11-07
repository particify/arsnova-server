package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class ScaleChoiceContent extends ChoiceQuestionContent {
  public enum ScaleOptionTemplate {
    AGREEMENT,
    INTENSITY,
    FREQUENCY,
    QUALITY,
    PROBABILITY,
    LEVEL,
    IMPORTANCE,
    DIFFICULTY,
    PACE,
    EMOJI,
    PLUS_MINUS,
    POINTS
  }

  @NotNull
  private ScaleOptionTemplate optionTemplate;

  @Min(3)
  @Max(11)
  private int optionCount;

  public ScaleChoiceContent() {
    super.setFormat(Format.SCALE);
  }

  public ScaleChoiceContent(final ScaleChoiceContent content) {
    super(content);
    this.optionTemplate = content.optionTemplate;
    this.optionCount = content.optionCount;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public ScaleOptionTemplate getOptionTemplate() {
    return optionTemplate;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setOptionTemplate(final ScaleOptionTemplate optionTemplate) {
    this.optionTemplate = optionTemplate;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getOptionCount() {
    return optionCount;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setOptionCount(final int optionCount) {
    this.optionCount = optionCount;
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public void setFormat(final Format format) {
    // Detect invalid use of the API
    if (format != Format.SCALE) {
      throw new IllegalArgumentException("Format must be 'SCALE' for this type.");
    }
    // The actual field will not be updated
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public void setOptions(final List<AnswerOption> options) {
    // Detect invalid use of the API
    if (options != null && !options.isEmpty()) {
      throw new IllegalArgumentException("Options must be an empty list for this format.");
    }
    // The actual field will not be updated
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public void setCorrectOptionIndexes(final List<Integer> correctOptionIndexes) {
    // Check to detect invalid use of the API
    if (correctOptionIndexes != null && !correctOptionIndexes.isEmpty()) {
      throw new IllegalArgumentException("Options must be an empty list for this format.");
    }
    // The actual field will not be updated
  }
}
