/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.common.exception.InvalidInputException

/**
 * Format data of a content whose answers can be excluded from the stats by banning keywords. A
 * keyword matches a text by their normalized forms.
 */
interface KeywordBanning : ContentFormatData {
  var bannedKeywords: MutableList<String>

  /**
   * A keyword is banned by its normalized form, so variants of a banned one are not added again.
   */
  fun addBannedKeyword(keyword: String) {
    val normalizedKeyword = normalizeAnswerText(keyword)
    if (normalizedKeyword.isEmpty() || keyword.length > MAX_KEYWORD_LENGTH) {
      throw InvalidInputException(
          "A keyword must contain a letter or digit and not exceed $MAX_KEYWORD_LENGTH characters")
    }
    if (bannedKeywords.none { normalizeAnswerText(it) == normalizedKeyword }) {
      if (bannedKeywords.size >= MAX_KEYWORD_COUNT) {
        throw InvalidInputException("No more than $MAX_KEYWORD_COUNT keywords can be banned")
      }
      bannedKeywords.add(keyword)
    }
  }

  fun resetBannedKeywords() {
    bannedKeywords = mutableListOf()
  }

  companion object {
    /** Long enough for any answer text a banning format accepts. */
    const val MAX_KEYWORD_LENGTH = 80
    const val MAX_KEYWORD_COUNT = 100
  }
}
