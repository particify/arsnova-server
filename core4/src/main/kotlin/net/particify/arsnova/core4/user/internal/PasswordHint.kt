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
 * zxcvbn only words its warnings for the names of its own dictionaries, so a word from those of
 * zxcvbn-ts is explained under the name of the built-in dictionary of the same kind. The kind is
 * the part of the name before the language.
 */
private val FEEDBACK_DICTIONARY_NAMES =
    mapOf(
        "boysFirstnames" to "male_names",
        "femaleFirstnames" to "female_names",
        "femaleLastnames" to "surnames",
        "firstnames" to "male_names",
        "girlsFirstnames" to "female_names",
        "lastnames" to "surnames",
        "maleFirstnames" to "male_names",
        "maleLastnames" to "surnames",
        "names" to "surnames",
        "passwords" to "passwords")

/** Commonly used words, Wikipedia's and word sequences such as the months. */
private const val FEEDBACK_WORD_DICTIONARY_NAME = "english_wikipedia"

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
  val sequence = strength.sequence.map(::withFeedbackDictionaryName)
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

private fun withFeedbackDictionaryName(match: Match): Match {
  val name = match.dictionaryName
  if (match.pattern != Pattern.Dictionary ||
      name?.startsWith(ADDITIONAL_DICTIONARY_PREFIX) != true) {
    return match
  }
  val kind = name.removePrefix(ADDITIONAL_DICTIONARY_PREFIX).substringBefore("-")
  return Match.Builder(match.pattern, match.i, match.j, match.token)
      .matchedWord(match.matchedWord)
      .rank(match.rank)
      .dictionaryName(FEEDBACK_DICTIONARY_NAMES[kind] ?: FEEDBACK_WORD_DICTIONARY_NAME)
      .reversed(match.reversed)
      .l33t(match.l33t)
      .sub(match.sub)
      .subDisplay(match.subDisplay)
      .guesses(match.guesses)
      .guessesLog10(match.guessesLog10)
      .build()
}
