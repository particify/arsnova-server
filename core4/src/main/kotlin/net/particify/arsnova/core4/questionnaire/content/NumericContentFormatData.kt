/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

data class NumericContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @JsonProperty("minNumber") val minNumber: Double,
    @JsonProperty("maxNumber") val maxNumber: Double,
    @JsonProperty("correctNumber") val correctNumber: Double? = null,
    @JsonProperty("tolerance") val tolerance: Double = 0.0,
) : ContentFormatData {
  override val format = "NUMERIC"

  override fun supportsRounds() = true

  override fun hasCorrectAnswer(): Boolean = correctNumber != null

  override fun withoutSolution() = copy(correctNumber = null)

  override fun validate() {
    if (minNumber > maxNumber) {
      throw ContentFormatDataInvalidException("Minimum must not be greater than maximum")
    }
    if (tolerance < 0) {
      throw ContentFormatDataInvalidException("Tolerance must not be negative")
    }
    if (correctNumber != null && correctNumber !in minNumber..maxNumber) {
      throw ContentFormatDataInvalidException("Correct number must be within the range")
    }
  }

  /**
   * An answer's result and points are decided when it is given and never recomputed, so changing
   * what counts as correct afterwards would leave stored results disagreeing with the content. The
   * range stays editable: it does not take part in scoring and stored answers keep their values.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? NumericContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a numeric content")
    val scoringChanged =
        currentFormatData.correctNumber != correctNumber || currentFormatData.tolerance != tolerance
    if (scoringChanged && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "The correct number and tolerance must not be changed once the content has answers")
    }
  }

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    if ((answerFormatData as NumericAnswerFormatData).selectedNumber !in minNumber..maxNumber) {
      throw ContentFormatDataInvalidException(
          "Answer must be between $minNumber and $maxNumber for this numeric content")
    }
  }

  /** Both tolerance bounds are inclusive. */
  fun isCorrect(selectedNumber: Double): Boolean {
    val correct = correctNumber ?: return false
    return selectedNumber >= correct - tolerance && selectedNumber <= correct + tolerance
  }

  /**
   * An answer of another format is neither correct nor wrong: a content's format can be changed
   * while it holds answers, and scoring a stored answer must not fail the summary it is part of.
   */
  override fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState {
    if (!hasCorrectAnswer()) return AnswerResultState.NEUTRAL
    val numericAnswerFormatData =
        answerFormatData as? NumericAnswerFormatData ?: return AnswerResultState.NEUTRAL
    return if (isCorrect(numericAnswerFormatData.selectedNumber)) AnswerResultState.CORRECT
    else AnswerResultState.WRONG
  }
}
