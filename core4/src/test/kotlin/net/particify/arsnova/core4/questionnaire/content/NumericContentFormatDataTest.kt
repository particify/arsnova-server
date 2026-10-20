/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BASE_POINTS = 500

class NumericContentFormatDataTest {
  // Bounds and tolerance are exactly representable, so the inclusive bounds are tested as such.
  private val withTolerance = NumericContentFormatData(0.0, 100.0, 10.0, 2.5)
  private val withoutTolerance = NumericContentFormatData(0.0, 100.0, 10.0)
  private val withoutCorrectNumber = NumericContentFormatData(0.0, 100.0)

  @Test
  fun shouldSupportRoundsAndScoreOnlyWithCorrectNumber() {
    Assertions.assertTrue(withTolerance.supportsRounds())
    Assertions.assertTrue(withTolerance.hasCorrectAnswer())
    Assertions.assertFalse(withoutCorrectNumber.hasCorrectAnswer())
  }

  @Test
  fun shouldAcceptOnlyExactNumberWithoutTolerance() {
    Assertions.assertTrue(withoutTolerance.isCorrect(10.0))
    Assertions.assertFalse(withoutTolerance.isCorrect(9.99))
    Assertions.assertFalse(withoutTolerance.isCorrect(10.01))
  }

  @Test
  fun shouldAcceptNumbersWithinInclusiveToleranceBounds() {
    listOf(7.5, 9.0, 10.0, 12.5).forEach {
      Assertions.assertTrue(withTolerance.isCorrect(it), "$it")
    }
    listOf(7.49, 12.51).forEach { Assertions.assertFalse(withTolerance.isCorrect(it), "$it") }
  }

  @Test
  fun shouldNeverBeCorrectWithoutCorrectNumber() {
    Assertions.assertFalse(withoutCorrectNumber.isCorrect(0.0))
    val answer = NumericAnswerFormatData(0.0)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, withoutCorrectNumber.evaluateAnswer(answer))
  }

  @Test
  fun shouldScoreAllOrNothing() {
    val correct = NumericAnswerFormatData(12.0)
    val wrong = NumericAnswerFormatData(13.0)
    Assertions.assertEquals(AnswerResultState.CORRECT, withTolerance.evaluateAnswer(correct))
    Assertions.assertEquals(AnswerResultState.WRONG, withTolerance.evaluateAnswer(wrong))
    Assertions.assertEquals(
        BASE_POINTS, withTolerance.calculateAchievedPoints(correct, BASE_POINTS))
    Assertions.assertEquals(0, withTolerance.calculateAchievedPoints(wrong, BASE_POINTS))
  }

  @Test
  fun shouldScoreAnswerOfOtherFormatNeutrallyAndRejectItOnSubmission() {
    val foreign = TextAnswerFormatData("10")
    Assertions.assertEquals(AnswerResultState.NEUTRAL, withTolerance.evaluateAnswer(foreign))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      withTolerance.validateAnswer(foreign)
    }
  }

  @Test
  fun shouldAllowAnyChangeWithinTheFormatWithoutAnswers() {
    NumericContentFormatData(-5.0, 50.0, 20.0, 1.0).validateChange(withTolerance) { false }
  }

  @Test
  fun shouldKeepRangeEditableOnceAnswered() {
    NumericContentFormatData(-5.0, 50.0, 10.0, 2.5).validateChange(withTolerance) { true }
  }

  @Test
  fun shouldLockCorrectNumberAndToleranceOnceAnswered() {
    listOf(
            NumericContentFormatData(0.0, 100.0, 11.0, 2.5),
            NumericContentFormatData(0.0, 100.0, 10.0, 3.0),
            NumericContentFormatData(0.0, 100.0, null, 2.5))
        .forEach {
          Assertions.assertThrows(
              ContentFormatDataInvalidException::class.java,
              { it.validateChange(withTolerance) { true } },
              "$it")
        }
  }

  @Test
  fun shouldAcceptContentWithCorrectNumberAtRangeBounds() {
    NumericContentFormatData(0.0, 100.0, 0.0).validate()
    NumericContentFormatData(0.0, 100.0, 100.0).validate()
    NumericContentFormatData(5.0, 5.0, 5.0).validate()
    withoutCorrectNumber.validate()
  }

  @Test
  fun shouldRejectInvalidContent() {
    listOf(
            NumericContentFormatData(10.0, 0.0),
            NumericContentFormatData(0.0, 10.0, 5.0, -0.5),
            NumericContentFormatData(0.0, 10.0, -1.0),
            NumericContentFormatData(0.0, 10.0, 10.5))
        .forEach {
          Assertions.assertThrows(
              ContentFormatDataInvalidException::class.java, { it.validate() }, "$it")
        }
  }

  @Test
  fun shouldAcceptAnswersWithinTheRangeOnly() {
    listOf(0.0, 42.0, 100.0).forEach { withTolerance.validateAnswer(NumericAnswerFormatData(it)) }
    listOf(-0.1, 100.1).forEach {
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { withTolerance.validateAnswer(NumericAnswerFormatData(it)) },
          "$it")
    }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      withTolerance.validateChange(ChoiceContentFormatData(listOf("A"), null, false)) { false }
    }
  }

  @Test
  fun shouldDropOnlyTheCorrectNumberWhenWithholdingTheSolution() {
    Assertions.assertEquals(
        NumericContentFormatData(0.0, 100.0, null, 2.5), withTolerance.withoutSolution())
    Assertions.assertEquals(10.0, withTolerance.correctNumber)
  }
}
