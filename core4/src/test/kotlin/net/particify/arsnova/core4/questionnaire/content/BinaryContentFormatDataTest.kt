/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.BinaryAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BASE_POINTS = 500

class BinaryContentFormatDataTest {
  private val yes = BinaryAnswerFormatData(true)
  private val no = BinaryAnswerFormatData(false)

  @Test
  fun shouldSupportRoundsAndScoreOnlyWithCorrectAnswer() {
    Assertions.assertTrue(BinaryContentFormatData(true).supportsRounds())
    Assertions.assertTrue(BinaryContentFormatData().supportsRounds())
    Assertions.assertTrue(BinaryContentFormatData(false).hasCorrectAnswer())
    Assertions.assertFalse(BinaryContentFormatData().hasCorrectAnswer())
  }

  @Test
  fun shouldScoreTheMatchingAnswerAsCorrectAllOrNothing() {
    listOf(true, false).forEach { correctAnswer ->
      val formatData = BinaryContentFormatData(correctAnswer)
      val (right, wrong) = if (correctAnswer) yes to no else no to yes
      Assertions.assertEquals(AnswerResultState.CORRECT, formatData.evaluateAnswer(right))
      Assertions.assertEquals(AnswerResultState.WRONG, formatData.evaluateAnswer(wrong))
      Assertions.assertEquals(BASE_POINTS, formatData.calculateAchievedPoints(right, BASE_POINTS))
      Assertions.assertEquals(0, formatData.calculateAchievedPoints(wrong, BASE_POINTS))
    }
  }

  @Test
  fun shouldScoreNeutrallyWithoutCorrectAnswer() {
    Assertions.assertEquals(
        AnswerResultState.NEUTRAL, BinaryContentFormatData().evaluateAnswer(yes))
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      BinaryContentFormatData(true).validateAnswer(ChoiceAnswerFormatData(listOf(0)))
    }
  }

  @Test
  fun shouldEvaluateStoredAnswerOfOtherFormatAsNeutral() {
    Assertions.assertEquals(
        AnswerResultState.NEUTRAL,
        BinaryContentFormatData(true).evaluateAnswer(ChoiceAnswerFormatData(listOf(0))))
  }

  @Test
  fun shouldAllowAnyChangeWithoutAnswers() {
    BinaryContentFormatData(false).validateChange(BinaryContentFormatData(true)) { false }
    BinaryContentFormatData(null).validateChange(BinaryContentFormatData(true)) { false }
  }

  @Test
  fun shouldNotConsultAnswersForAnUnchangedCorrectAnswer() {
    BinaryContentFormatData(true).validateChange(BinaryContentFormatData(true)) {
      Assertions.fail("Answers must not be looked up for an unchanged correct answer")
    }
  }

  @Test
  fun shouldLockTheCorrectAnswerOnceAnswered() {
    listOf(BinaryContentFormatData(false), BinaryContentFormatData(null)).forEach {
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { it.validateChange(BinaryContentFormatData(true)) { true } },
          "$it")
    }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      BinaryContentFormatData(true).validateChange(TextContentFormatData(false)) { false }
    }
  }

  @Test
  fun shouldWithholdTheCorrectAnswer() {
    val stored = BinaryContentFormatData(true)
    Assertions.assertEquals(BinaryContentFormatData(null), stored.withoutSolution())
    Assertions.assertEquals(true, stored.correctAnswer)
  }
}
