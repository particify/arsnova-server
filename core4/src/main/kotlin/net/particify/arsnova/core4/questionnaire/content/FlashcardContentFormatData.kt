/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

data class FlashcardContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("answer") val answer: String) : ContentFormatData {
  override val format = "FLASHCARD"

  override fun withoutRuntimeState() = this

  override fun answerable() = false

  /** An update of another format would reinterpret the content's rounds and answers. */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    if (current !is FlashcardContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a flashcard content")
    }
  }

  override fun validate() {
    if (answer.isBlank()) {
      throw ContentFormatDataInvalidException("Flashcard answer must not be blank")
    }
    if (answer.length > MAX_ANSWER_LENGTH) {
      throw ContentFormatDataInvalidException(
          "Flashcard answer must not exceed $MAX_ANSWER_LENGTH characters")
    }
  }

  companion object {
    const val MAX_ANSWER_LENGTH = 2000
  }
}
