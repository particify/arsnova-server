/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

data class ScaleContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @JsonProperty("template") val template: Template,
    @JsonProperty("neutralOption") val neutralOption: Boolean,
) : ContentFormatData {
  override val format = "SCALE"

  override fun withoutRuntimeState() = this

  override fun supportsRounds() = true

  fun optionCount(): Int =
      if (neutralOption) OPTION_COUNT_WITH_NEUTRAL else OPTION_COUNT_WITHOUT_NEUTRAL

  /**
   * A rating is an index into the options the template displays, so changing the template or the
   * neutral option once answers exist would silently reinterpret every one of them.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? ScaleContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a scale content")
    if (currentFormatData != this && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "Scale template and neutral option must not be changed once the content has answers")
    }
  }

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val optionCount = optionCount()
    if ((answerFormatData as ScaleAnswerFormatData).rating !in 0 until optionCount) {
      throw ContentFormatDataInvalidException(
          "Rating must be between 0 and ${optionCount - 1} for this scale content")
    }
  }

  enum class Template {
    AGREEMENT,
    INTENSITY,
    FREQUENCY,
    QUALITY,
    PROBABILITY,
    LEVEL,
    IMPORTANCE,
    DIFFICULTY,
    PACE,
    EMOJI,
    PLUS_MINUS,
    POINTS
  }

  companion object {
    const val OPTION_COUNT_WITHOUT_NEUTRAL = 4
    const val OPTION_COUNT_WITH_NEUTRAL = 5
  }
}
