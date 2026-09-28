/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
@JsonSubTypes(
    JsonSubTypes.Type(ChoiceContentFormatData::class, name = "CHOICE"),
    JsonSubTypes.Type(FlashcardContentFormatData::class, name = "FLASHCARD"),
    JsonSubTypes.Type(SlideContentFormatData::class, name = "SLIDE"),
    JsonSubTypes.Type(WordcloudContentFormatData::class, name = "WORDCLOUD"),
)
interface ContentFormatData {
  val format: String

  fun hasCorrectAnswer(): Boolean = false

  fun answerable(): Boolean = true

  /** Whether a content of this format may be answered again in a new round. Formats opt in. */
  fun supportsRounds(): Boolean = false

  /** Rejects format data that is invalid on its own terms, on create and on update alike. */
  fun validate() {}

  /**
   * Rejects an update that would reinterpret answers already given. [hasAnswers] hits the database,
   * so only consult it once the format data is known to differ.
   */
  fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {}

  /** Carries over state the server owns and the client does not send back. */
  fun mergeWith(current: ContentFormatData): ContentFormatData = this

  /**
   * The format data as shown to someone who may not see the solution yet. Only ever serialized for
   * that reader, never persisted, so it may drop what the scoring needs.
   */
  fun withoutSolution(): ContentFormatData = this

  fun validateAnswer(answerFormatData: AnswerFormatData) {
    if (answerFormatData.format != format) {
      throw ContentFormatDataInvalidException(
          "Answer format ${answerFormatData.format} does not match content format $format")
    }
  }

  fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState =
      AnswerResultState.NEUTRAL

  /**
   * The share of [basePoints] the answer is worth. Formats which cannot be answered partially score
   * all or nothing.
   */
  fun calculateAchievedPoints(answerFormatData: AnswerFormatData, basePoints: Int): Int =
      if (evaluateAnswer(answerFormatData) == AnswerResultState.CORRECT) basePoints else 0
}
