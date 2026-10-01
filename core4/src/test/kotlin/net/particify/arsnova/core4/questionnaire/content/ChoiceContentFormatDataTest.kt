/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BASE_POINTS = 500
private val OPTIONS = listOf("A", "B", "C")

class ChoiceContentFormatDataTest {
  private fun result(correctOptionIndexes: List<Int>?, vararg selectedChoiceIndexes: Int) =
      ChoiceContentFormatData(OPTIONS, correctOptionIndexes, true)
          .evaluateAnswer(ChoiceAnswerFormatData(selectedChoiceIndexes.toList()))

  @Test
  fun shouldEvaluateSelectionOfExactlyTheCorrectOptionsAsCorrect() {
    Assertions.assertEquals(AnswerResultState.CORRECT, result(listOf(0, 2), 0, 2))
  }

  @Test
  fun shouldEvaluateSelectionOfOnlyWrongOptionsAsWrong() {
    Assertions.assertEquals(AnswerResultState.WRONG, result(listOf(0, 2), 1))
  }

  @Test
  fun shouldEvaluateNeutrallyWithoutCorrectOptions() {
    Assertions.assertEquals(AnswerResultState.NEUTRAL, result(null, 1))
    Assertions.assertEquals(AnswerResultState.NEUTRAL, result(listOf(), 1))
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      ChoiceContentFormatData(OPTIONS, null, false)
          .validateAnswer(WordcloudAnswerFormatData(listOf("A")))
    }
  }

  @Test
  fun shouldIgnoreTheOrderOfSelectedOptions() {
    Assertions.assertEquals(AnswerResultState.CORRECT, result(listOf(0, 2), 2, 0))
  }

  @Test
  fun shouldScoreSingleChoiceAllOrNothing() {
    val singleChoice = ChoiceContentFormatData(OPTIONS, listOf(1), false)
    val right = ChoiceAnswerFormatData(listOf(1))
    val wrong = ChoiceAnswerFormatData(listOf(0))
    Assertions.assertEquals(BASE_POINTS, singleChoice.calculateAchievedPoints(right, BASE_POINTS))
    Assertions.assertEquals(0, singleChoice.calculateAchievedPoints(wrong, BASE_POINTS))
  }

  @Test
  fun shouldRequireEverySelectionWhenAllOptionsAreCorrect() {
    val allCorrect = ChoiceContentFormatData(OPTIONS, OPTIONS.indices.toList(), true)
    val everyOption = ChoiceAnswerFormatData(OPTIONS.indices.toList())
    Assertions.assertEquals(AnswerResultState.CORRECT, allCorrect.evaluateAnswer(everyOption))
    Assertions.assertEquals(
        BASE_POINTS, allCorrect.calculateAchievedPoints(everyOption, BASE_POINTS))
  }

  @Test
  fun shouldScoreAnEmptySelectionAsWrongWithoutPoints() {
    val content = ChoiceContentFormatData(OPTIONS, listOf(0), true)
    val nothing = ChoiceAnswerFormatData(listOf())
    Assertions.assertEquals(AnswerResultState.WRONG, content.evaluateAnswer(nothing))
    Assertions.assertEquals(0, content.calculateAchievedPoints(nothing, BASE_POINTS))
  }

  @Test
  fun shouldAwardNoPointsForEmptyCorrectOptions() {
    val content = ChoiceContentFormatData(OPTIONS, listOf(), true)
    Assertions.assertEquals(
        0, content.calculateAchievedPoints(ChoiceAnswerFormatData(listOf(0)), BASE_POINTS))
  }

  @Test
  fun shouldOnlyReportACorrectAnswerWithCorrectOptions() {
    Assertions.assertFalse(ChoiceContentFormatData(OPTIONS, null, true).hasCorrectAnswer())
    Assertions.assertFalse(ChoiceContentFormatData(OPTIONS, listOf(), true).hasCorrectAnswer())
    Assertions.assertTrue(ChoiceContentFormatData(OPTIONS, listOf(1), true).hasCorrectAnswer())
  }

  @Test
  fun shouldAcceptDistinctSelectionsWithinTheOptions() {
    ChoiceContentFormatData(OPTIONS, null, true).validateAnswer(choice(2, 0, 1))
    ChoiceContentFormatData(OPTIONS, null, false).validateAnswer(choice(2))
    ChoiceContentFormatData(OPTIONS, null, false).validateAnswer(choice())
  }

  @Test
  fun shouldRejectSelectionOutsideTheOptions() {
    assertRejected(ChoiceContentFormatData(OPTIONS, null, true), choice(0, OPTIONS.size))
    assertRejected(ChoiceContentFormatData(OPTIONS, null, true), choice(-1))
  }

  @Test
  fun shouldRejectRepeatedSelection() {
    assertRejected(ChoiceContentFormatData(OPTIONS, listOf(0, 1, 2), true), choice(0, 0, 0))
  }

  @Test
  fun shouldRejectSeveralSelectionsForSingleChoice() {
    assertRejected(ChoiceContentFormatData(OPTIONS, null, false), choice(0, 1))
    assertRejected(ChoiceContentFormatData(OPTIONS, null, null), choice(0, 1))
  }

  private fun choice(vararg selectedChoiceIndexes: Int) =
      ChoiceAnswerFormatData(selectedChoiceIndexes.toList())

  private fun assertRejected(content: ChoiceContentFormatData, answer: ChoiceAnswerFormatData) {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      content.validateAnswer(answer)
    }
  }

  @Test
  fun shouldDropOnlyTheCorrectOptionsWhenWithholdingTheSolution() {
    val stored = ChoiceContentFormatData(OPTIONS, listOf(0, 2), true)
    Assertions.assertEquals(ChoiceContentFormatData(OPTIONS, null, true), stored.withoutSolution())
    Assertions.assertEquals(listOf(0, 2), stored.correctOptionIndexes)
  }

  @Test
  fun shouldKeepFormatDataWithoutSolutionAsIs() {
    val wordcloud = WordcloudContentFormatData()
    Assertions.assertSame(wordcloud, wordcloud.withoutSolution())
  }
}
