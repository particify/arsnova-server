/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class NonAnswerableContentFormatDataTest {
  @Test
  fun shouldNeitherBeAnswerableNorScorableNorSupportRounds() {
    listOf(FlashcardContentFormatData("Back"), SlideContentFormatData()).forEach {
      Assertions.assertFalse(it.answerable(), it.format)
      Assertions.assertFalse(it.hasCorrectAnswer(), it.format)
      Assertions.assertFalse(it.supportsRounds(), it.format)
    }
  }

  @Test
  fun shouldAcceptFlashcardAnswerUpToMaximumLength() {
    FlashcardContentFormatData("x").validate()
    FlashcardContentFormatData("x".repeat(FlashcardContentFormatData.MAX_ANSWER_LENGTH)).validate()
  }

  @Test
  fun shouldRejectBlankFlashcardAnswer() {
    listOf("", " ", "\n\t").forEach {
      Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
        FlashcardContentFormatData(it).validate()
      }
    }
  }

  @Test
  fun shouldRejectFlashcardAnswerAboveMaximumLength() {
    val tooLong = "x".repeat(FlashcardContentFormatData.MAX_ANSWER_LENGTH + 1)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      FlashcardContentFormatData(tooLong).validate()
    }
  }

  @Test
  fun shouldAcceptSlideWithoutFurtherData() {
    SlideContentFormatData().validate()
  }

  @Test
  fun shouldAllowChangesWithinTheSameFormat() {
    FlashcardContentFormatData("New").validateChange(FlashcardContentFormatData("Old")) { true }
    SlideContentFormatData().validateChange(SlideContentFormatData()) { true }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    val choice = ChoiceContentFormatData(listOf("A", "B"), listOf(0), false)
    listOf(
            FlashcardContentFormatData("Back") to choice,
            FlashcardContentFormatData("Back") to SlideContentFormatData(),
            SlideContentFormatData() to choice,
            SlideContentFormatData() to FlashcardContentFormatData("Back"))
        .forEach { (formatData, current) ->
          Assertions.assertThrows(
              ContentFormatDataInvalidException::class.java,
              { formatData.validateChange(current) { false } },
              "${formatData.format} over ${current.format}")
        }
  }
}
