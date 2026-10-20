/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

private val SPECIAL_CHAR_REGEX = Regex("[^\\p{IsAlphabetic}\\p{IsDigit}]")

/**
 * Folds a free-text answer to the form used to compare answers with each other and with a content's
 * correct terms: lowercase, with everything that is neither a letter nor a digit removed. Answers
 * differing only in case, punctuation or spacing therefore compare equal.
 */
fun normalizeAnswerText(text: String): String = text.lowercase().replace(SPECIAL_CHAR_REGEX, "")
