/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * The fewest words each directory generated from the zxcvbn-ts packages is expected to hold, about
 * nine tenths of what the pinned versions contain. A package whose layout the build no longer
 * understands yields far fewer.
 */
private val MIN_WORDS =
    mapOf(
        "ar" to 48_000,
        "common" to 50_000,
        "cs" to 78_000,
        "da" to 410_000,
        "de" to 80_000,
        "en" to 160_000,
        "es" to 88_000,
        "fa" to 19_000,
        "fi" to 52_000,
        "fr" to 71_000,
        "hr" to 45_000,
        "id" to 32_000,
        "it" to 76_000,
        "ja" to 25_000,
        "ko" to 1_100,
        "ku" to 1_400,
        "nl" to 52_000,
        "pl" to 108_000,
        "pt" to 66_000,
        "ro" to 114_000,
        "ru" to 100_000,
        "th" to 8_200,
        "tr" to 37_000,
        "zh" to 35_000)

/**
 * A common word of each language whose lists are not ASCII, as typed. Most others are romanized.
 */
private val NON_ASCII_WORDS = mapOf("ko" to "가야겠습니다", "th" to "ครอบครัว", "tr" to "teşekkürler")

/** How much a dictionary match must lower the estimate below that of random characters. */
private const val DICTIONARY_MATCH_MIN_GAIN_LOG10 = 3

class PasswordDictionariesTests {
  @Test
  fun shouldGenerateEveryLanguage() {
    Assertions.assertEquals(MIN_WORDS.keys - "common", availableDictionaryLanguages.toSet())
  }

  @Test
  fun shouldGenerateCompleteDictionaries() {
    MIN_WORDS.forEach { (directory, minWords) ->
      val words = loadDictionaries(directory).sumOf { it.frequencies.size }
      Assertions.assertTrue(words >= minWords, "$directory has $words words, expected $minWords")
    }
  }

  @Test
  fun shouldMatchNonAsciiWords() {
    val withoutLanguages = passwordEstimator(listOf("en", "de"))
    val withLanguages = unsharedPasswordEstimator(listOf("en", "de") + NON_ASCII_WORDS.keys)
    NON_ASCII_WORDS.values.forEach {
      val gain = withoutLanguages.measure(it).guessesLog10 - withLanguages.measure(it).guessesLog10
      Assertions.assertTrue(gain >= DICTIONARY_MATCH_MIN_GAIN_LOG10, "$it: $gain")
    }
  }
}
