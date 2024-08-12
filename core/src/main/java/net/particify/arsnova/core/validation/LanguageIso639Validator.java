package net.particify.arsnova.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class LanguageIso639Validator implements ConstraintValidator<LanguageIso639, String> {
  private static final Set<String> languages = Set.of(Locale.getISOLanguages()).stream()
      .filter(l -> Locale.of(l).getLanguage().equals(l))
      .collect(Collectors.toSet());

  @Override
  public void initialize(final LanguageIso639 constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return value == null || languages.contains(value);
  }
}
