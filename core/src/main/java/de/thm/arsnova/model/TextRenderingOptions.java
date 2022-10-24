package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public class TextRenderingOptions {
  public enum MarkdownFeatureset {
    MINIMUM,
    SIMPLE,
    EXTENDED
  }

  private boolean linebreaksEnabled = true;
  private boolean markdownEnabled = true;
  private boolean latexEnabled = true;
  private MarkdownFeatureset markdownFeatureset = MarkdownFeatureset.SIMPLE;

  @JsonProperty("linebreaks")
  public boolean isLinebreaksEnabled() {
    return linebreaksEnabled;
  }

  public void setLinebreaksEnabled(final boolean linebreaksEnabled) {
    this.linebreaksEnabled = linebreaksEnabled;
  }

  @JsonProperty("markdown")
  public boolean isMarkdownEnabled() {
    return markdownEnabled;
  }

  public void setMarkdownEnabled(final boolean markdownEnabled) {
    this.markdownEnabled = markdownEnabled;
  }

  @JsonProperty("latex")
  public boolean isLatexEnabled() {
    return latexEnabled;
  }

  public void setLatexEnabled(final boolean latexEnabled) {
    this.latexEnabled = latexEnabled;
  }

  @JsonProperty()
  public MarkdownFeatureset getMarkdownFeatureset() {
    return markdownFeatureset;
  }

  public void setMarkdownFeatureset(final MarkdownFeatureset markdownFeatureset) {
    this.markdownFeatureset = markdownFeatureset;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TextRenderingOptions that = (TextRenderingOptions) o;

    return linebreaksEnabled == that.linebreaksEnabled
        && markdownEnabled == that.markdownEnabled
        && latexEnabled == that.latexEnabled
        && markdownFeatureset == that.markdownFeatureset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(linebreaksEnabled, markdownEnabled, latexEnabled, markdownFeatureset);
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
        .append("linebreaksEnabled", linebreaksEnabled)
        .append("markdownEnabled", markdownEnabled)
        .append("latexEnabled", latexEnabled)
        .append("markdownFeatureset", markdownFeatureset)
        .toString();
  }
}
