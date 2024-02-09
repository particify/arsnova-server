package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.serialization.View;

@RestController
@RequestMapping(LanguageController.REQUEST_MAPPING)
public class LanguageController {
  public static final String REQUEST_MAPPING = "/language";

  @GetMapping("/")
  public Set<Language> getLanguages(final HttpServletRequest request) {
    final String acceptLanguage = request.getHeader("Accept-Language");
    final Locale requestLocale = acceptLanguage != null && acceptLanguage.length() >= 2
        ? Locale.of(acceptLanguage.substring(0, 2))
        : Locale.of("en");
    return Arrays.stream(Locale.getISOLanguages())
        .map(lang -> Locale.of(lang))
        .map(locale -> new Language(
            locale.getLanguage(),
            locale.getDisplayLanguage(locale),
            locale.getDisplayLanguage(requestLocale)))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @JsonView(View.Public.class)
  private record Language(String code, String nativeName, String localizedName) implements Comparable<Language> {
    @Override
    public int compareTo(final Language language) {
      return this.code.compareTo(language.code);
    }
  }
}
