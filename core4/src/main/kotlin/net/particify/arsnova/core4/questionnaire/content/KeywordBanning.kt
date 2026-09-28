/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

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
    if (bannedKeywords.none { normalizeAnswerText(it) == normalizedKeyword }) {
      bannedKeywords.add(keyword)
    }
  }

  fun resetBannedKeywords() {
    bannedKeywords = mutableListOf()
  }
}
