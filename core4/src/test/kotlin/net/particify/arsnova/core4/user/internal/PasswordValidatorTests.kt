/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import net.particify.arsnova.core4.user.exception.InvalidPasswordException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

private const val PASSWORD = "sXQodP0UYeGQXuXE"

/** Line breaks, tab, NUL, escape, delete and a C1 control. */
private val CONTROL_CHARACTERS = listOf("\n", "\r", "\t", "\u0000", "\u001B", "\u007F", "\u0085")

/** ASCII space, tab, no-break space, em space, narrow no-break space, ideographic space. */
private val WHITESPACE = listOf(" ", "\t", " ", " ", " ", "　")

class PasswordValidatorTests {
  @Test
  fun shouldRejectControlCharacters() {
    CONTROL_CHARACTERS.forEach {
      assertThrows<InvalidPasswordException>(it) { PasswordValidator.check("sXQod${it}P0UYeGQ") }
    }
  }

  @Test
  fun shouldRejectLeadingWhitespace() {
    WHITESPACE.forEach {
      assertThrows<InvalidPasswordException>(it) { PasswordValidator.check(it + PASSWORD) }
    }
  }

  @Test
  fun shouldRejectTrailingWhitespace() {
    WHITESPACE.forEach {
      assertThrows<InvalidPasswordException>(it) { PasswordValidator.check(PASSWORD + it) }
    }
  }

  @Test
  fun shouldAcceptInnerSpaces() {
    listOf(" ", " ", "　").forEach {
      assertDoesNotThrow(it) { PasswordValidator.check("correct${it}horse${it}battery${it}staple") }
    }
  }

  @Test
  fun shouldAcceptCharactersBeyondAscii() {
    assertDoesNotThrow { PasswordValidator.check("Blümchen-Ωmega-密码-🔑") }
  }
}
