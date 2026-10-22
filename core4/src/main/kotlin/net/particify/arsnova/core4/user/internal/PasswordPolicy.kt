/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import java.net.URI
import java.util.Locale
import net.particify.arsnova.core4.common.ServiceProperties
import net.particify.arsnova.core4.user.exception.WeakPasswordException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * zxcvbn estimates a password of random characters at no more than 10 to the power of its length,
 * so with the default threshold a shorter one could never be accepted anyway.
 */
private const val PASSWORD_MIN_LENGTH = 10

/**
 * The estimate's cost grows much faster than the length: rating 72 characters takes about ten times
 * as long as rating 40. Characters past the limit are left out of the estimate, which can only make
 * a password look weaker than it is, never stronger.
 */
private const val PASSWORD_MEASURED_LENGTH = 40

/** Splits inputs into words, so that a password containing part of an input scores low too. */
private val INPUT_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
private const val INPUT_PART_MIN_LENGTH = 3

/** Strictly below 1, so that a full bar always means an accepted password. */
private val REJECTED_MAX_RELATIVE_STRENGTH = Math.nextDown(1.0)

/**
 * The strength of a password as presented to its user.
 *
 * @property relativeStrength how close the password is to the threshold, from 0 to 1, which it
 *   reaches only if [acceptable]
 * @property hint the most important advice for improving a rejected password
 */
data class PasswordStrength(
    val acceptable: Boolean,
    val relativeStrength: Double,
    val hint: String?
)

/**
 * Decides whether a password is strong enough to be set. The enforced state and the state reported
 * while a password is entered are both derived from it, so the two cannot drift apart.
 *
 * The estimator is loaded while the application starts rather than on the first check, since
 * reading its dictionaries takes a noticeable moment.
 */
@Component
class PasswordPolicy
internal constructor(
    properties: PasswordProperties,
    serviceProperties: ServiceProperties,
    estimator: (Collection<String>) -> Zxcvbn
) {
  @Autowired
  constructor(
      properties: PasswordProperties,
      serviceProperties: ServiceProperties
  ) : this(properties, serviceProperties, ::passwordEstimator)

  private val zxcvbn = estimator(properties.dictionaryLanguages)

  private val minGuessesLog10 = properties.minGuessesLog10

  /** Configured words plus the service's host name, passed to the estimate with every password. */
  private val contextInputs =
      withParts(properties.contextWords + listOfNotNull(URI(serviceProperties.rootUrl).host))

  /**
   * @param userInputs data of the account the password is for, such as its mail address and name,
   *   which the estimate treats as known to an attacker
   */
  fun checkStrength(password: String, vararg userInputs: String?) {
    if (!isAcceptable(password, measure(password, userInputs.toList()))) {
      throw WeakPasswordException()
    }
  }

  /** Same as [checkStrength], but reporting the result with a hint in the given language. */
  fun estimateStrength(
      password: String,
      userInputs: List<String?>,
      locale: Locale
  ): PasswordStrength {
    val strength = measure(password, userInputs)
    val relativeStrength =
        if (minGuessesLog10 == 0) 1.0 else minOf(strength.guessesLog10 / minGuessesLog10, 1.0)
    if (isAcceptable(password, strength)) {
      return PasswordStrength(true, relativeStrength, null)
    }
    val hint = if (password.length < PASSWORD_MIN_LENGTH) null else passwordHint(strength, locale)
    return PasswordStrength(false, minOf(relativeStrength, REJECTED_MAX_RELATIVE_STRENGTH), hint)
  }

  private fun measure(password: String, userInputs: List<String?>): Strength {
    val inputs = (contextInputs + withParts(userInputs.filterNotNull())).distinct()
    return zxcvbn.measure(password.take(PASSWORD_MEASURED_LENGTH), inputs)
  }

  private fun isAcceptable(password: String, strength: Strength) =
      password.length >= PASSWORD_MIN_LENGTH && strength.guessesLog10 >= minGuessesLog10

  /**
   * Adds the parts of each input, since a password rarely contains a mail address as a whole but
   * often a part of it. Parts too short to be a word are left out.
   */
  private fun withParts(inputs: List<String>): List<String> =
      inputs
          .flatMap { input ->
            listOf(input) +
                input.split(INPUT_SEPARATOR).filter { it.length >= INPUT_PART_MIN_LENGTH }
          }
          .filter { it.isNotBlank() }
          .distinct()
}
