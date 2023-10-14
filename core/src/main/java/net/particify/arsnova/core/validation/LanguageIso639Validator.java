package net.particify.arsnova.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Locale;

public class LanguageIso639Validator implements ConstraintValidator<LanguageIso639, String> {
  private static final List<String> languages = List.of(Locale.getISOLanguages());

  @Override
  public void initialize(final LanguageIso639 constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return value != null && languages.contains(value);
  }
}
