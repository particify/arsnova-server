/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

data class TextContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("answersPublished") val answersPublished: Boolean) : ContentFormatData {
  override val format = "TEXT"

  /**
   * Retains whether the answers are published. Publishing is a separate operation and must not be
   * reset by editing the content.
   */
  override fun mergeWith(current: ContentFormatData): ContentFormatData {
    val currentFormatData =
        current as? TextContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a text content")
    return TextContentFormatData(currentFormatData.answersPublished)
  }

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val body = (answerFormatData as TextAnswerFormatData).body
    if (body.isBlank()) {
      throw ContentFormatDataInvalidException("Text answer must not be blank")
    }
    if (body.length > MAX_ANSWER_LENGTH) {
      throw ContentFormatDataInvalidException(
          "Text answer must not exceed $MAX_ANSWER_LENGTH characters")
    }
  }

  companion object {
    const val MAX_ANSWER_LENGTH = 2000
  }
}
