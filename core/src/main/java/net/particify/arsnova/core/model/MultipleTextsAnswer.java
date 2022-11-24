package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class MultipleTextsAnswer extends Answer {
  private @NotNull List<@NotBlank String> texts = new ArrayList<>();

  public MultipleTextsAnswer() {

  }

  public MultipleTextsAnswer(final Content content, final String creatorId) {
    super(content, creatorId);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getTexts() {
    return texts;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTexts(final List<String> texts) {
    this.texts = texts;
  }

  @Override
  public boolean isAbstention() {
    return texts.isEmpty();
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>MultipleTextsAnswer</tt> are included in equality checks.
   * </p>
   */
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
    final MultipleTextsAnswer that = (MultipleTextsAnswer) o;

    return Objects.equals(texts, that.texts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), texts);
  }
}
