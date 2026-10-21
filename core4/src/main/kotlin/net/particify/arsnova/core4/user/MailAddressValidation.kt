/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component

/** The length of the mail address and username columns. */
const val MAIL_ADDRESS_MAX_LENGTH = 100

/**
 * Applies the constraints of a mail address argument to an address which does not arrive as one: an
 * address passed in by another module or asserted by an identity provider.
 *
 * Surrounding whitespace is rejected rather than trimmed, the same as on the mutations.
 */
@Component
class MailAddressValidation(private val validator: Validator) {
  fun isValid(mailAddress: String): Boolean = violations(mailAddress).isEmpty()

  /** @throws ConstraintViolationException which the API reports as a bad request. */
  fun validate(mailAddress: String) {
    val violations = violations(mailAddress)
    if (violations.isNotEmpty()) {
      throw ConstraintViolationException(violations)
    }
  }

  private fun violations(mailAddress: String) = validator.validate(MailAddress(mailAddress))

  private class MailAddress(
      @field:Email @field:Size(max = MAIL_ADDRESS_MAX_LENGTH) val mailAddress: String
  )
}
