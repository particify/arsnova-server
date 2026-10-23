/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import com.nulabinc.zxcvbn.Feedback
import com.nulabinc.zxcvbn.Pattern
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.matchers.Match
import java.util.Collections
import java.util.Enumeration
import java.util.Locale
import java.util.ResourceBundle

/**
 * zxcvbn explains a password only up to its score 2, whose upper bound this is, while the threshold
 * rejects passwords above it as well.
 */
private const val EXPLAINED_MAX_GUESSES = 1e8

/** zxcvbn adds it to the advice for every pattern, and gives it alone for none. */
private const val ADD_ANOTHER_WORD = Feedback.EXTRA_SUGGESTIONS_ADD_ANOTHER_WORD

/** Suggestions zxcvbn gives regardless of what it found in the password. */
private val GENERIC_SUGGESTIONS =
    setOf(
        ADD_ANOTHER_WORD,
        Feedback.DEFAULT_SUGGESTIONS_USE_FEW_WORDS,
        Feedback.DEFAULT_SUGGESTIONS_NO_NEED_SYMBOLS)

/**
 * Resolves every message to its key, which reveals the keys of a feedback through its public API.
 */
private object MessageKeys : ResourceBundle() {
  override fun handleGetObject(key: String): Any = key

  override fun getKeys(): Enumeration<String> = Collections.emptyEnumeration()
}

/**
 * zxcvbn's warning, else its first suggestion specific to what it found. Its generic advice to add
 * a word is only given if its feedback is about a pattern and has nothing more specific to say
 * about it, so null means that zxcvbn found no pattern worth explaining.
 */
internal fun passwordHint(strength: Strength, locale: Locale): String? {
  val sequence = strength.sequence
  val feedback =
      Strength(
              strength.password,
              minOf(strength.guesses, EXPLAINED_MAX_GUESSES),
              sequence,
              strength.calcTime)
          .feedback
  val keys = feedback.withResourceBundle(MessageKeys)
  if (keys.getWarning(locale).isNotEmpty()) {
    return feedback.getWarning(locale)
  }
  val suggestionKeys = keys.getSuggestions(locale)
  val specific = suggestionKeys.indexOfFirst { it !in GENERIC_SUGGESTIONS }
  val index =
      when {
        specific >= 0 -> specific
        explainsPattern(sequence) -> suggestionKeys.indexOf(ADD_ANOTHER_WORD)
        else -> -1
      }
  return if (index >= 0) feedback.getSuggestions(locale)[index] else null
}

/**
 * Whether zxcvbn's feedback is about a pattern: it only considers the longest match, the first of
 * them if several are equally long, which can be a stretch of random characters.
 */
private fun explainsPattern(sequence: List<Match>): Boolean {
  val longest = sequence.reduceOrNull { a, b -> if (b.tokenLength() > a.tokenLength()) b else a }
  return longest != null && longest.pattern != Pattern.Bruteforce
}
