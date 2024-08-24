package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class ShortAnswer extends Answer {
  // Validation: null is allowed for abstentions
  @Size(min = 1, max = 80)
  private String text;

  @JsonView({View.Persistence.class, View.Public.class})
  public String getText() {
    return text;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setText(final String text) {
    this.text = text;
  }

  @Override
  public boolean isAbstention() {
    return text == null;
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
    final ShortAnswer that = (ShortAnswer) o;
    return Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), text);
  }
}
