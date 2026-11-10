/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

open class LanguageIso639Validator : ConstraintValidator<LanguageIso639, String?> {
  override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
    return value == null || LANGUAGE_ISO_639_CODES.contains(value)
  }
}
