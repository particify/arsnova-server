package net.particify.arsnova.core.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(AliasWordsProperties.PREFIX)
public class AliasWordsProperties {
  public static final String PREFIX = "alias-words";

  private List<String> adjectives;
  private List<String> nouns;
  private List<String> specialNouns;

  @ConstructorBinding
  public AliasWordsProperties(
      final List<String> adjectives,
      final List<String> nouns,
      final List<String> specialNouns) {
    this.adjectives = adjectives;
    this.nouns = nouns;
    this.specialNouns = specialNouns;
  }

  public List<String> getAdjectives() {
    return adjectives;
  }

  public List<String> getNouns() {
    return nouns;
  }

  public List<String> getSpecialNouns() {
    return specialNouns;
  }
}
