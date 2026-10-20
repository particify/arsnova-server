/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TextContentFormatDataTest {
  @Test
  fun shouldBeAnswerableWithoutCorrectAnswerOrRounds() {
    val formatData = TextContentFormatData(false)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertFalse(formatData.supportsRounds())
  }

  @Test
  fun shouldKeepPublishedStateOfCurrentContentOnUpdate() {
    listOf(true, false).forEach { published ->
      listOf(true, false).forEach { incoming ->
        val merged = TextContentFormatData(incoming).mergeWith(TextContentFormatData(published))
        Assertions.assertEquals(
            TextContentFormatData(published), merged, "$incoming over $published")
      }
    }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      TextContentFormatData(false).mergeWith(ChoiceContentFormatData(listOf("A"), null, false))
    }
  }

  @Test
  fun shouldAcceptTextAnswerAndRejectAnswerOfOtherFormat() {
    val formatData = TextContentFormatData(false)
    formatData.validateAnswer(TextAnswerFormatData("An answer"))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      formatData.validateAnswer(ChoiceAnswerFormatData(listOf(0)))
    }
  }

  @Test
  fun shouldAcceptTextAnswerUpToMaximumLength() {
    TextContentFormatData(false)
        .validateAnswer(TextAnswerFormatData("x".repeat(TextContentFormatData.MAX_ANSWER_LENGTH)))
  }

  @Test
  fun shouldRejectBlankTextAnswer() {
    listOf("", " ", "\n\t").forEach {
      Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
        TextContentFormatData(false).validateAnswer(TextAnswerFormatData(it))
      }
    }
  }

  @Test
  fun shouldRejectTextAnswerAboveMaximumLength() {
    val tooLong = TextAnswerFormatData("x".repeat(TextContentFormatData.MAX_ANSWER_LENGTH + 1))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      TextContentFormatData(false).validateAnswer(tooLong)
    }
  }
}
