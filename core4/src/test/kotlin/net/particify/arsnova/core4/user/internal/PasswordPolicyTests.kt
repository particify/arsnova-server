/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Duration
import java.util.Locale
import net.particify.arsnova.core4.user.exception.WeakPasswordException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/** Random, so that nothing but its length and character set make it hard to guess. */
private const val STRONG_PASSWORD = "sXQodP0UYeGQXuXE"

/** A threshold the strong password and both keyboard walks below are estimated far apart from. */
private const val KEYBOARD_WALK_MIN_GUESSES_LOG10 = 9

private const val LONG_PASSWORD_LENGTH = 10_000
private const val RELATIVE_STRENGTH_TOLERANCE = 0.01

/** Every case the issue names as weak, plus common words combined with digits or a name. */
private val REJECTED_PASSWORDS =
    listOf(
        "abcXYZ123!",
        "AaBbCc123",
        "1qay2wsx",
        "P@ssw0rd",
        "Aaaaaaaa",
        "Sommer2024!",
        "Coffeecup99",
        "Michaelsmith",
        "Sunflower12")
private const val CAPITALIZATION_HINT = "Capitalization doesn't help very much."
private const val SIMILAR_PASSWORD_HINT = "This is similar to a commonly used password."

/**
 * The English hint of each. Where zxcvbn has nothing specific to say about the pattern it found, it
 * advises adding words.
 */
private val HINTED_PASSWORDS =
    mapOf(
        "abcXYZ123!" to SIMILAR_PASSWORD_HINT,
        "arsnovaparticify" to "Add another word or two. Uncommon words are better.",
        "Dragonfly1!" to CAPITALIZATION_HINT,
        "Michaelsmith" to "Common names and surnames are easy to guess.",
        "Passwort123" to SIMILAR_PASSWORD_HINT,
        "qwertzuiop1" to "Straight rows of keys are easy to guess.",
        "Television" to "A word by itself is easy to guess.")

/** Shorter than the minimum, which the client explains itself. */
private val SHORT_PASSWORDS = listOf("AaBbCc123", "1qay2wsx", "P@ssw0rd", "Aaaaaaaa")

/** Random, and exactly as long as the minimum. */
private const val SHORTEST_STRONG_PASSWORD = "kX9#mQ2pLw"
private val LONG_PASSWORD_TIMEOUT = Duration.ofSeconds(5)

class PasswordPolicyTests {
  private val policy = passwordPolicy()

  @Test
  fun shouldRejectGuessablePasswords() {
    listOf("abcXYZ123!", "AaBbCc123", "1qay2wsx", "P@ssw0rd", "Aaaaaaaa", "Sommer2024!").forEach {
      assertThrows<WeakPasswordException>(it) { policy.checkStrength(it) }
    }
  }

  @Test
  fun shouldAcceptRandomPassword() {
    assertDoesNotThrow { policy.checkStrength(STRONG_PASSWORD) }
  }

  @Test
  fun shouldRejectShortPasswordWhateverItsEstimate() {
    assertThrows<WeakPasswordException> {
      passwordPolicy(minGuessesLog10 = 0).checkStrength(STRONG_PASSWORD.take(9))
    }
  }

  @Test
  fun shouldRejectPasswordMadeOfUserInputs() {
    val password = "QuorvexBlandimor"
    assertDoesNotThrow { policy.checkStrength(password) }
    assertThrows<WeakPasswordException> { policy.checkStrength(password, "Quorvex", "Blandimor") }
  }

  /** A password rarely contains the whole address, but often a part of it. */
  @Test
  fun shouldRejectPasswordMadeOfPartOfUserInput() {
    val password = "Blandimor2024!x"
    assertDoesNotThrow { policy.checkStrength(password, "Quorvex") }
    assertThrows<WeakPasswordException> {
      policy.checkStrength(password, "quorvex.blandimor@example.org")
    }
  }

  @Test
  fun shouldRejectPasswordMadeOfPartOfHostName() {
    val password = "Zentavio2024!x"
    assertDoesNotThrow { policy.checkStrength(password) }
    assertThrows<WeakPasswordException> {
      passwordPolicy(rootUrl = "https://ars.zentavio.example").checkStrength(password)
    }
  }

  @Test
  fun shouldIgnoreMissingUserInputs() {
    assertDoesNotThrow { policy.checkStrength(STRONG_PASSWORD, null, "", " ") }
  }

  @Test
  fun shouldRejectPasswordMadeOfContextWords() {
    val password = "arsnovaparticify"
    assertDoesNotThrow { passwordPolicy(contextWords = listOf()).checkStrength(password) }
    assertThrows<WeakPasswordException> { policy.checkStrength(password) }
  }

  @Test
  fun shouldRejectHostNameOfService() {
    val password = "Zentavio.example"
    assertDoesNotThrow { policy.checkStrength(password) }
    assertThrows<WeakPasswordException> {
      passwordPolicy(rootUrl = "https://zentavio.example/app").checkStrength(password)
    }
  }

  /** Both are well above the threshold for an estimate which only knows QWERTY. */
  @Test
  fun shouldRecogniseQwertzAndAzertyKeyboardWalks() {
    val keyboardPolicy = passwordPolicy(minGuessesLog10 = KEYBOARD_WALK_MIN_GUESSES_LOG10)
    listOf("yxcvbnm,.-#äö", "&é\"'(-è_çà").forEach {
      assertThrows<WeakPasswordException>(it) { keyboardPolicy.checkStrength(it) }
    }
  }

  @Test
  fun shouldEstimateLongPasswordInBoundedTime() {
    val password = STRONG_PASSWORD.repeat(LONG_PASSWORD_LENGTH / STRONG_PASSWORD.length)
    Assertions.assertTimeoutPreemptively(LONG_PASSWORD_TIMEOUT) { policy.checkStrength(password) }
  }

  @Test
  fun shouldReportAcceptedPasswordAsComplete() {
    Assertions.assertEquals(
        PasswordStrength(true, 1.0, null),
        policy.estimateStrength(STRONG_PASSWORD, listOf(), Locale.ENGLISH))
  }

  @Test
  fun shouldReportRelativeStrengthTowardsThreshold() {
    val strength = policy.estimateStrength("Aaaaaaaa", listOf(), Locale.ENGLISH)
    Assertions.assertEquals(0.405, strength.relativeStrength, RELATIVE_STRENGTH_TOLERANCE)
  }

  /** Some cases lie above the guesses up to which zxcvbn itself gives any feedback. */
  @Test
  fun shouldExplainEveryRejectedPassword() {
    val estimates =
        REJECTED_PASSWORDS.associateWith { policy.estimateStrength(it, listOf(), Locale.ENGLISH) }
    estimates.forEach { (password, strength) ->
      Assertions.assertFalse(strength.acceptable, password)
      Assertions.assertTrue(strength.relativeStrength < 1.0, password)
      Assertions.assertEquals(
          password in SHORT_PASSWORDS, strength.hint.isNullOrBlank(), "$password: ${strength.hint}")
    }
  }

  @Test
  fun shouldAcceptRandomPasswordOfMinimumLength() {
    assertDoesNotThrow { policy.checkStrength(SHORTEST_STRONG_PASSWORD) }
    Assertions.assertTrue(
        policy.estimateStrength(SHORTEST_STRONG_PASSWORD, listOf(), Locale.ENGLISH).acceptable)
  }

  @Test
  fun shouldGiveNoHintBelowMinimumLength() {
    SHORT_PASSWORDS.forEach {
      val strength = policy.estimateStrength(it, listOf(), Locale.ENGLISH)
      Assertions.assertFalse(strength.acceptable, it)
      Assertions.assertNull(strength.hint, it)
    }
  }

  @Test
  fun shouldGiveSpecificHints() {
    val hints =
        HINTED_PASSWORDS.keys.associateWith {
          policy.estimateStrength(it, listOf(), Locale.ENGLISH).hint
        }
    Assertions.assertEquals(HINTED_PASSWORDS, hints)
  }

  /**
   * zxcvbn's feedback is about the longest match, here random characters after a short sequence.
   */
  @Test
  fun shouldGiveNoHintWithoutPattern() {
    val strength = policy.estimateStrength("gdaiuzxbts", listOf(), Locale.ENGLISH)
    Assertions.assertFalse(strength.acceptable)
    Assertions.assertNull(strength.hint)
  }

  @Test
  fun shouldReportShortPasswordWithoutHint() {
    val strength = policy.estimateStrength(STRONG_PASSWORD.take(9), listOf(), Locale.ENGLISH)
    Assertions.assertFalse(strength.acceptable)
    Assertions.assertTrue(strength.relativeStrength < 1.0)
    Assertions.assertNull(strength.hint)
  }

  @Test
  fun shouldNeverReportShortPasswordAsComplete() {
    val strength =
        passwordPolicy(minGuessesLog10 = 0)
            .estimateStrength(STRONG_PASSWORD.take(9), listOf(), Locale.ENGLISH)
    Assertions.assertFalse(strength.acceptable)
    Assertions.assertTrue(strength.relativeStrength < 1.0)
  }

  @Test
  fun shouldLocalizeHint() {
    Assertions.assertEquals(
        "This is similar to a commonly used password.",
        policy.estimateStrength("Passwort123", listOf(), Locale.ENGLISH).hint)
    Assertions.assertEquals(
        "Das ist sehr ähnlich zu einem häufig verwendeten Passwort.",
        policy.estimateStrength("Passwort123", listOf(), Locale.GERMAN).hint)
  }

  /** zxcvbn has no Czech messages, and English is its base language rather than the server's. */
  @Test
  fun shouldFallBackToEnglishHint() {
    Assertions.assertEquals(
        policy.estimateStrength("Passwort123", listOf(), Locale.ENGLISH).hint,
        policy.estimateStrength("Passwort123", listOf(), Locale.forLanguageTag("cs")).hint)
  }

  @Test
  fun shouldEstimateWithUserInputs() {
    val strength =
        policy.estimateStrength(
            "Blandimor2024!x", listOf(null, "quorvex.blandimor@example.org"), Locale.ENGLISH)
    Assertions.assertFalse(strength.acceptable)
  }
}
