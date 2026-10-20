/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BASE_POINTS = 500
private const val THIRD_OF_BASE_POINTS = 167
private const val TWO_THIRDS_OF_BASE_POINTS = 333
private val OPTIONS = listOf("A", "B", "C", "D")

// Seven correct options are a case whose shares do not add up to exactly one in binary floating
// point, which is what the correctness threshold exists for.
private const val INEXACT_OPTION_COUNT = 7
private const val INEXACT_BASE_POINTS = 700
private const val SIX_SEVENTHS_OF_INEXACT_BASE_POINTS = 600

class ChoicePartialCreditTest {
  private val content = ChoiceContentFormatData(OPTIONS, listOf(0, 1, 2), true)
  private val inexactContent =
      ChoiceContentFormatData(
          (0 until INEXACT_OPTION_COUNT).map { it.toString() },
          (0 until INEXACT_OPTION_COUNT).toList(),
          true)

  private fun achievedPoints(vararg selectedChoiceIndexes: Int) =
      content.calculateAchievedPoints(
          ChoiceAnswerFormatData(selectedChoiceIndexes.toList()), BASE_POINTS)

  private fun result(vararg selectedChoiceIndexes: Int) =
      content.evaluateAnswer(ChoiceAnswerFormatData(selectedChoiceIndexes.toList()))

  @Test
  fun shouldAwardAllPointsForAllCorrectOptions() {
    Assertions.assertEquals(BASE_POINTS, achievedPoints(0, 1, 2))
    Assertions.assertEquals(AnswerResultState.CORRECT, result(0, 1, 2))
  }

  @Test
  fun shouldAwardAShareForEachCorrectOption() {
    Assertions.assertEquals(THIRD_OF_BASE_POINTS, achievedPoints(0))
    Assertions.assertEquals(TWO_THIRDS_OF_BASE_POINTS, achievedPoints(0, 1))
  }

  @Test
  fun shouldSubtractAShareForEachWrongOption() {
    Assertions.assertEquals(TWO_THIRDS_OF_BASE_POINTS, achievedPoints(0, 1, 2, 3))
    Assertions.assertEquals(0, achievedPoints(0, 3))
  }

  @Test
  fun shouldNotAwardNegativePointsForMoreWrongThanCorrectOptions() {
    Assertions.assertEquals(0, achievedPoints(3))
    Assertions.assertEquals(0, achievedPoints(0, 3, 3))
  }

  @Test
  fun shouldOnlyCountAFullyCorrectAnswerAsCorrect() {
    Assertions.assertEquals(AnswerResultState.WRONG, result(0, 1))
    Assertions.assertEquals(AnswerResultState.WRONG, result(0, 1, 2, 3))
    Assertions.assertEquals(AnswerResultState.WRONG, result(3))
  }

  @Test
  fun shouldScoreNeutrallyWithoutCorrectOptions() {
    val unscorable = ChoiceContentFormatData(OPTIONS, null, false)
    val answer = ChoiceAnswerFormatData(listOf(0))
    Assertions.assertEquals(AnswerResultState.NEUTRAL, unscorable.evaluateAnswer(answer))
    Assertions.assertEquals(0, unscorable.calculateAchievedPoints(answer, BASE_POINTS))
  }

  @Test
  fun shouldScoreAnAnswerOfAnotherFormatWithoutFailing() {
    val foreignAnswer = WordcloudAnswerFormatData(listOf("A"))
    Assertions.assertEquals(0, content.calculateAchievedPoints(foreignAnswer, BASE_POINTS))
    Assertions.assertEquals(AnswerResultState.WRONG, content.evaluateAnswer(foreignAnswer))
  }

  @Test
  fun shouldCountAnAnswerJustShortOfTheFullShareAsCorrect() {
    val answer = ChoiceAnswerFormatData((0 until INEXACT_OPTION_COUNT).toList())
    Assertions.assertEquals(AnswerResultState.CORRECT, inexactContent.evaluateAnswer(answer))
    Assertions.assertEquals(
        INEXACT_BASE_POINTS, inexactContent.calculateAchievedPoints(answer, INEXACT_BASE_POINTS))
  }

  @Test
  fun shouldNotCountAnAnswerBelowTheThresholdAsCorrect() {
    val answer = ChoiceAnswerFormatData((0 until INEXACT_OPTION_COUNT - 1).toList())
    Assertions.assertEquals(AnswerResultState.WRONG, inexactContent.evaluateAnswer(answer))
    Assertions.assertEquals(
        SIX_SEVENTHS_OF_INEXACT_BASE_POINTS,
        inexactContent.calculateAchievedPoints(answer, INEXACT_BASE_POINTS))
  }
}
