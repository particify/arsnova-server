/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.PrioritizationAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PrioritizationContentFormatDataTest {
  private val formatData = PrioritizationContentFormatData(listOf("First", "Second", "Third"))

  private fun points(vararg assignedPoints: Int) =
      PrioritizationAnswerFormatData(assignedPoints.toList())

  private fun assertRejected(executable: () -> Unit, message: String) {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java, executable, message)
  }

  @Test
  fun shouldOfferTheFixedBudgetWithoutScoringOrRounds() {
    Assertions.assertEquals(
        PrioritizationContentFormatData.ASSIGNABLE_POINTS, formatData.assignablePoints)
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertFalse(formatData.supportsRounds())
    Assertions.assertEquals(AnswerResultState.NEUTRAL, formatData.evaluateAnswer(points(100, 0, 0)))
    Assertions.assertSame(formatData, formatData.withoutSolution())
  }

  @Test
  fun shouldAcceptAtLeastTwoDistinctOptions() {
    PrioritizationContentFormatData(listOf("A", "B")).validate()
    formatData.validate()
  }

  @Test
  fun shouldRejectTooFewBlankOrRepeatedOptions() {
    listOf(
            listOf(),
            listOf("A"),
            listOf("A", " "),
            listOf("A", "B", "A"),
            listOf("A", "A "),
            listOf("Apple", "apple"))
        .forEach { assertRejected({ PrioritizationContentFormatData(it).validate() }, "$it") }
  }

  @Test
  fun shouldAcceptOptionsAtTheUpperLimits() {
    val options =
        (1..PrioritizationContentFormatData.MAX_OPTION_COUNT).map {
          "$it".padEnd(PrioritizationContentFormatData.MAX_OPTION_LENGTH, 'x')
        }
    PrioritizationContentFormatData(options).validate()
  }

  @Test
  fun shouldRejectTooManyOrTooLongOptions() {
    val tooMany = (0..PrioritizationContentFormatData.MAX_OPTION_COUNT).map { "Option $it" }
    val tooLong = listOf("A", "B".repeat(PrioritizationContentFormatData.MAX_OPTION_LENGTH + 1))
    listOf(tooMany, tooLong).forEach {
      assertRejected({ PrioritizationContentFormatData(it).validate() }, "${it.size}")
    }
  }

  @Test
  fun shouldAcceptAnswersSpendingExactlyTheBudget() {
    listOf(points(100, 0, 0), points(50, 30, 20), points(0, 0, 100)).forEach {
      formatData.validateAnswer(it)
    }
  }

  @Test
  fun shouldRejectAnswersMissingTheBudgetOrTheOptions() {
    listOf(
            points(50, 50),
            points(25, 25, 25, 25),
            points(110, -10, 0),
            points(101, 0, 0),
            points(50, 30, 19),
            points(50, 30, 21))
        .forEach { assertRejected({ formatData.validateAnswer(it) }, "${it.assignedPoints}") }
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    assertRejected({ formatData.validateAnswer(SortAnswerFormatData(listOf(0, 1, 2))) }, "sort")
  }

  @Test
  fun shouldAllowAnyChangeWithoutAnswers() {
    PrioritizationContentFormatData(listOf("Third", "Fourth")).validateChange(formatData) { false }
  }

  @Test
  fun shouldNotConsultAnswersForUnchangedOptions() {
    PrioritizationContentFormatData(listOf("First", "Second", "Third")).validateChange(formatData) {
      Assertions.fail("Answers must not be looked up for unchanged options")
    }
  }

  @Test
  fun shouldLockOptionsIncludingTheirOrderOnceAnswered() {
    listOf(listOf("Second", "First", "Third"), listOf("First", "Second", "Fourth")).forEach {
      assertRejected(
          { PrioritizationContentFormatData(it).validateChange(formatData) { true } }, "$it")
    }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    assertRejected(
        { formatData.validateChange(SortContentFormatData(listOf("A", "B"))) { false } },
        "other format")
  }
}
