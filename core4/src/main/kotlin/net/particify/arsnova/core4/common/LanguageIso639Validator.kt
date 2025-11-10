/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Locale

open class LanguageIso639Validator : ConstraintValidator<LanguageIso639, String?> {
  override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
    return value == null || languages.contains(value)
  }

  companion object {
    private val languages =
        Locale.getISOLanguages().filter { it == Locale.of(it).language }.toMutableSet()
  }
}
