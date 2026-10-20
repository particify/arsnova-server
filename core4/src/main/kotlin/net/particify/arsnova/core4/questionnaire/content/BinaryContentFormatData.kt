/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.BinaryAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

/**
 * The yes and no labels are not stored: clients resolve them from their own translations, so the
 * format only needs the optionally declared [correctAnswer].
 */
data class BinaryContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("correctAnswer") val correctAnswer: Boolean? = null) : ContentFormatData {
  override val format = "BINARY"

  override fun supportsRounds() = true

  override fun hasCorrectAnswer(): Boolean = correctAnswer != null

  override fun withoutSolution() = copy(correctAnswer = null)

  /**
   * An answer's result and points are decided when it is given and never recomputed, so moving the
   * correct answer afterwards would leave stored results disagreeing with the content.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? BinaryContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a binary content")
    if (currentFormatData.correctAnswer != correctAnswer && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "The correct answer must not be changed once the content has answers")
    }
  }

  override fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState {
    val answer = (answerFormatData as? BinaryAnswerFormatData)?.answer
    return when {
      answer == null || correctAnswer == null -> AnswerResultState.NEUTRAL
      answer == correctAnswer -> AnswerResultState.CORRECT
      else -> AnswerResultState.WRONG
    }
  }
}
