package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.particify.arsnova.core.model.serialization.View;

public abstract class WordContent extends Content {
  private static final Pattern specialCharPattern = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]");

  @Size(max = 50)
  private Set<@NotBlank String> bannedKeywords;

  public WordContent() {
  }

  public WordContent(final WordContent content) {
    super(content);
    this.bannedKeywords = content.bannedKeywords;
  }

  @JsonView(View.Persistence.class)
  public Set<String> getBannedKeywords() {
    if (bannedKeywords == null) {
      bannedKeywords = new HashSet<>();
    }

    return bannedKeywords;
  }

  @JsonView(View.Persistence.class)
  public void setBannedKeywords(final Set<String> bannedKeywords) {
    this.bannedKeywords = bannedKeywords;
  }

  public WordContent addBannedKeyword(final String keyword) {
    this.getBannedKeywords().add(normalizeText(keyword));
    return this;
  }

  public void clearBannedKeywords() {
    this.bannedKeywords.clear();
  }

  public static String normalizeText(final String text) {
    return specialCharPattern.matcher(text.toLowerCase()).replaceAll("");
  }
}
