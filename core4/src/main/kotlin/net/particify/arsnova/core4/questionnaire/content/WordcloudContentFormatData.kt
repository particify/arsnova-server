/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

class WordcloudContentFormatData(val maxAnswers: Int) : KeywordBanning {
  override val format = "WORDCLOUD"
  override var bannedKeywords: MutableList<String> = mutableListOf()

  override fun supportsRounds() = true

  override fun validate() {
    if (maxAnswers < 1) {
      throw ContentFormatDataInvalidException("A wordcloud content must allow at least one word")
    }
  }

  /** An update of another format would reinterpret the content's rounds and answers. */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    if (current !is WordcloudContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a wordcloud content")
    }
  }

  /** Banned keywords are set through their own operations, never through an update. */
  override fun mergeWith(current: ContentFormatData): ContentFormatData {
    val merged = WordcloudContentFormatData(maxAnswers)
    if (current is WordcloudContentFormatData) {
      merged.bannedKeywords = current.bannedKeywords.toMutableList()
    }
    return merged
  }

  override fun withoutRuntimeState(): ContentFormatData = WordcloudContentFormatData(maxAnswers)

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val words = (answerFormatData as WordcloudAnswerFormatData).words
    if (words.size > maxAnswers) {
      throw ContentFormatDataInvalidException(
          "An answer must not contain more than $maxAnswers words")
    }
    if (words.any { it.isBlank() }) {
      throw ContentFormatDataInvalidException("Words must not be blank")
    }
    if (words.any { it.length > MAX_WORD_LENGTH }) {
      throw ContentFormatDataInvalidException("Words must not exceed $MAX_WORD_LENGTH characters")
    }
  }

  /** Equality covers the banned keywords, which are state rather than settings. */
  override fun equals(other: Any?): Boolean =
      other is WordcloudContentFormatData &&
          maxAnswers == other.maxAnswers &&
          bannedKeywords == other.bannedKeywords

  override fun hashCode(): Int = 31 * maxAnswers + bannedKeywords.hashCode()

  companion object {
    const val MAX_ANSWERS_DEFAULT = 3

    /** Matches the input limit of the answer form in the web client. */
    const val MAX_WORD_LENGTH = 25
  }

  constructor() : this(MAX_ANSWERS_DEFAULT)
}
