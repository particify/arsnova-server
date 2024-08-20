package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class WordcloudContent extends WordContent {

  @Min(1)
  @Max(10)
  private int maxAnswers = 1;

  public WordcloudContent() {

  }

  public WordcloudContent(final WordcloudContent content) {
    super(content);
    this.maxAnswers = content.maxAnswers;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getMaxAnswers() {
    return maxAnswers;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setMaxAnswers(final int maxAnswers) {
    this.maxAnswers = maxAnswers;
  }

  @Override
  public WordcloudContent copy() {
    return new WordcloudContent(this);
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
    final WordcloudContent that = (WordcloudContent) o;
    return maxAnswers == that.maxAnswers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), maxAnswers);
  }
}
