/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ScaleContentFormatDataTest {
  private val withNeutral = ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true)
  private val withoutNeutral =
      ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, false)

  @Test
  fun shouldCountFiveOptionsWithNeutralOptionAndFourWithout() {
    Assertions.assertEquals(5, withNeutral.optionCount())
    Assertions.assertEquals(4, withoutNeutral.optionCount())
  }

  @Test
  fun shouldSupportRoundsWithoutScoring() {
    Assertions.assertTrue(withNeutral.supportsRounds())
    Assertions.assertFalse(withNeutral.hasCorrectAnswer())
    Assertions.assertEquals(
        AnswerResultState.NEUTRAL, withNeutral.evaluateAnswer(ScaleAnswerFormatData(0)))
  }

  @Test
  fun shouldAcceptEveryRatingOfTheDisplayedOptions() {
    listOf(withNeutral, withoutNeutral).forEach { formatData ->
      (0 until formatData.optionCount()).forEach {
        formatData.validateAnswer(ScaleAnswerFormatData(it))
      }
    }
  }

  @Test
  fun shouldRejectRatingOutsideTheDisplayedOptions() {
    val cases = listOf(withNeutral to -1, withNeutral to 5, withoutNeutral to 4)
    cases.forEach { (formatData, rating) ->
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { formatData.validateAnswer(ScaleAnswerFormatData(rating)) },
          "$rating of ${formatData.optionCount()}")
    }
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      withNeutral.validateAnswer(TextAnswerFormatData("Agree"))
    }
  }

  @Test
  fun shouldAllowAnyChangeWithoutAnswers() {
    val changed = ScaleContentFormatData(ScaleContentFormatData.Template.PACE, false)
    changed.validateChange(withNeutral) { false }
  }

  @Test
  fun shouldNotConsultAnswersForAnUnchangedScale() {
    val unchanged = ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true)
    unchanged.validateChange(withNeutral) {
      Assertions.fail("Answers must not be looked up for an unchanged scale")
    }
  }

  @Test
  fun shouldLockTemplateAndNeutralOptionOnceAnswered() {
    listOf(withoutNeutral, ScaleContentFormatData(ScaleContentFormatData.Template.PACE, true))
        .forEach {
          Assertions.assertThrows(
              ContentFormatDataInvalidException::class.java,
              { it.validateChange(withNeutral) { true } },
              "$it")
        }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      withNeutral.validateChange(ChoiceContentFormatData(listOf("A"), null, false)) { false }
    }
  }
}
