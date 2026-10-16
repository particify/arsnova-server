/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import kotlin.random.Random
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BASE_POINTS = 500
private const val SEED = 42

class SortContentFormatDataTest {
  private val formatData = SortContentFormatData(listOf("First", "Second", "Third"))

  private fun sort(vararg selectedChoiceIndexes: Int) =
      SortAnswerFormatData(selectedChoiceIndexes.toList())

  private fun assertRejected(executable: () -> Unit, message: String) {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java, executable, message)
  }

  @Test
  fun shouldAlwaysHaveACorrectAnswerButNoRounds() {
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertFalse(formatData.supportsRounds())
  }

  @Test
  fun shouldAcceptAtLeastTwoDistinctOptions() {
    SortContentFormatData(listOf("A", "B")).validate()
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
        .forEach { assertRejected({ SortContentFormatData(it).validate() }, "$it") }
  }

  @Test
  fun shouldAcceptOptionsAtTheUpperLimits() {
    val options =
        (1..SortContentFormatData.MAX_OPTION_COUNT).map {
          "$it".padEnd(SortContentFormatData.MAX_OPTION_LENGTH, 'x')
        }
    SortContentFormatData(options).validate()
  }

  @Test
  fun shouldRejectTooManyOrTooLongOptions() {
    val tooMany = (0..SortContentFormatData.MAX_OPTION_COUNT).map { "Option $it" }
    val tooLong = listOf("A", "B".repeat(SortContentFormatData.MAX_OPTION_LENGTH + 1))
    listOf(tooMany, tooLong).forEach {
      assertRejected({ SortContentFormatData(it).validate() }, "${it.size}")
    }
  }

  @Test
  fun shouldAcceptEveryPermutationOfTheOptions() {
    listOf(sort(0, 1, 2), sort(2, 0, 1), sort(1, 2, 0)).forEach { formatData.validateAnswer(it) }
  }

  @Test
  fun shouldRejectAnswersThatAreNoPermutationOfTheOptions() {
    listOf(sort(), sort(0, 1), sort(0, 1, 1), sort(0, 1, 3), sort(0, 1, 2, 3)).forEach {
      assertRejected({ formatData.validateAnswer(it) }, "$it")
    }
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    assertRejected({ formatData.validateAnswer(ChoiceAnswerFormatData(listOf(0))) }, "choice")
  }

  @Test
  fun shouldScoreOnlyTheDeclaredOrderAsCorrect() {
    Assertions.assertEquals(AnswerResultState.CORRECT, formatData.evaluateAnswer(sort(0, 1, 2)))
    Assertions.assertEquals(AnswerResultState.WRONG, formatData.evaluateAnswer(sort(0, 2, 1)))
    Assertions.assertEquals(
        BASE_POINTS, formatData.calculateAchievedPoints(sort(0, 1, 2), BASE_POINTS))
    Assertions.assertEquals(0, formatData.calculateAchievedPoints(sort(1, 0, 2), BASE_POINTS))
  }

  @Test
  fun shouldAllowAnyChangeWithoutAnswers() {
    SortContentFormatData(listOf("Third", "First")).validateChange(formatData) { false }
  }

  @Test
  fun shouldNotConsultAnswersForUnchangedOptions() {
    SortContentFormatData(listOf("First", "Second", "Third")).validateChange(formatData) {
      Assertions.fail("Answers must not be looked up for unchanged options")
    }
  }

  @Test
  fun shouldLockOptionsIncludingTheirOrderOnceAnswered() {
    listOf(listOf("Second", "First", "Third"), listOf("First", "Second", "Fourth")).forEach {
      assertRejected({ SortContentFormatData(it).validateChange(formatData) { true } }, "$it")
    }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    assertRejected(
        { formatData.validateChange(TextContentFormatData(false)) { false } }, "other format")
  }

  @Test
  fun shouldStoreShuffledOptionsWithTheirCorrectSequence() {
    val correctlyOrdered = (1..8).map { "Option $it" }
    val shuffled = SortContentFormatData.shuffled(correctlyOrdered, Random(SEED))
    Assertions.assertEquals(correctlyOrdered.sorted(), shuffled.options.sorted())
    Assertions.assertNotEquals(correctlyOrdered, shuffled.options)
    Assertions.assertEquals(correctlyOrdered, shuffled.correctlyOrderedOptions())
    shuffled.validate()
    val correctOrder = shuffled.correctOrder!!
    Assertions.assertEquals(
        AnswerResultState.CORRECT, shuffled.evaluateAnswer(sort(*correctOrder.toIntArray())))
    Assertions.assertEquals(
        AnswerResultState.WRONG,
        shuffled.evaluateAnswer(sort(*shuffled.options.indices.toList().toIntArray())))
  }

  @Test
  fun shouldReadOptionsStoredWithoutCorrectOrderAsAlreadyInTheirCorrectSequence() {
    Assertions.assertEquals(
        SortContentFormatData(listOf("A", "B"), listOf(0, 1)),
        SortContentFormatData.fromJson(listOf("A", "B"), null))
  }

  @Test
  fun shouldRejectCorrectOrderThatIsNoPermutationOfTheOptions() {
    listOf(listOf(0, 1), listOf(0, 1, 1), listOf(0, 1, 3)).forEach {
      assertRejected({ SortContentFormatData(formatData.options, it).validate() }, "$it")
    }
  }

  @Test
  fun shouldKeepTheStoredOrderWhileTheCorrectSequenceIsUnchanged() {
    val stored = SortContentFormatData(listOf("Third", "First", "Second"), listOf(1, 2, 0))
    Assertions.assertSame(stored, formatData.mergeWith(stored))
    val changed = SortContentFormatData(listOf("Second", "First", "Third"))
    Assertions.assertSame(changed, changed.mergeWith(stored))
  }

  @Test
  fun shouldNotConsultAnswersForAnotherStorageOfTheSameSequence() {
    val stored = SortContentFormatData(listOf("Third", "First", "Second"), listOf(1, 2, 0))
    formatData.validateChange(stored) {
      Assertions.fail("Answers must not be looked up for an unchanged sequence")
    }
  }

  @Test
  fun shouldWithholdOnlyTheCorrectOrder() {
    val stored = SortContentFormatData(listOf("Third", "First", "Second"), listOf(1, 2, 0))
    Assertions.assertEquals(
        SortContentFormatData(listOf("Third", "First", "Second"), null), stored.withoutSolution())
    Assertions.assertEquals(listOf(1, 2, 0), stored.correctOrder)
  }
}
