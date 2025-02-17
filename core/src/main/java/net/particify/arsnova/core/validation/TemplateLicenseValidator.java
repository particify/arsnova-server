package net.particify.arsnova.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

import net.particify.arsnova.core.config.properties.PresetsProperties;

public class TemplateLicenseValidator implements ConstraintValidator<TemplateLicense, String> {
  private List<String> licenses;

  public TemplateLicenseValidator(final PresetsProperties presetsProperties) {
    licenses = presetsProperties.getTemplateLicenses();
  }

  @Override
  public void initialize(final TemplateLicense constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return value == null || licenses.contains(value);
  }
}
