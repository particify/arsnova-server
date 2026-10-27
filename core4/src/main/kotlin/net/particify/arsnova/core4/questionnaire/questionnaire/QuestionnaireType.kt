/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData

enum class QuestionnaireType {
  MIXED,
  QUIZ,
  SURVEY,
  FLASHCARDS;

  /**
   * Whether a content fits this type of questionnaire: a quiz takes only contents with a correct
   * answer and a survey only contents without one. Slides fit everywhere.
   */
  fun accepts(formatData: ContentFormatData): Boolean =
      formatData is SlideContentFormatData ||
          when (this) {
            MIXED -> true
            FLASHCARDS -> formatData is FlashcardContentFormatData
            QUIZ -> formatData.format in QUIZ_FORMATS && formatData.hasCorrectAnswer()
            SURVEY -> formatData.format in SURVEY_FORMATS && !formatData.hasCorrectAnswer()
          }

  companion object {
    private val QUIZ_FORMATS = setOf("BINARY", "CHOICE", "NUMERIC", "SHORT_ANSWER", "SORT")
    private val SURVEY_FORMATS =
        setOf("BINARY", "CHOICE", "NUMERIC", "PRIORITIZATION", "SCALE", "TEXT", "WORDCLOUD")
  }
}
