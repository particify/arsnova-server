/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import net.particify.arsnova.core4.user.exception.InvalidPasswordException

/**
 * Rejects passwords that a login form cannot reproduce reliably: one with a control character
 * cannot be typed, and whitespace at either end is easily lost or added without notice. Applied
 * only when a password is set, so that existing passwords keep working.
 */
object PasswordValidator {
  fun check(password: String) {
    if (password.isNotEmpty() && (hasControlCharacter(password) || hasOuterWhitespace(password))) {
      throw InvalidPasswordException()
    }
  }

  private fun hasControlCharacter(password: String) =
      password.codePoints().anyMatch { isControl(it) }

  private fun hasOuterWhitespace(password: String) =
      isWhitespace(password.codePointAt(0)) ||
          isWhitespace(password.codePointBefore(password.length))

  private fun isControl(codePoint: Int) = Character.getType(codePoint) == Character.CONTROL.toInt()

  /** Covers the space separators, such as the no-break space, which isWhitespace leaves out. */
  private fun isWhitespace(codePoint: Int) =
      Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
}
