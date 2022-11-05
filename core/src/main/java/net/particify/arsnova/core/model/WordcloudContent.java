package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.particify.arsnova.core.model.serialization.View;

public class WordcloudContent extends Content {
  private static final Pattern specialCharPattern = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]");

  @Min(1)
  @Max(10)
  private int maxAnswers = 1;

  @Size(max = 50)
  private Set<@NotBlank String> bannedKeywords;

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

  public WordcloudContent addBannedKeyword(final String keyword) {
    this.getBannedKeywords().add(normalizeText(keyword));
    return this;
  }

  public static String normalizeText(final String text) {
    return specialCharPattern.matcher(text.toLowerCase()).replaceAll("");
  }
}
