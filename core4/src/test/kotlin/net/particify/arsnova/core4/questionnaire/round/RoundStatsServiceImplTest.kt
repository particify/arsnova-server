/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

private val ROUND_ID = UUID.randomUUID()

class RoundStatsServiceImplTest {
  private val roundServiceImpl = Mockito.mock(RoundServiceImpl::class.java)
  private val answerServiceImpl = Mockito.mock(AnswerServiceImpl::class.java)
  private val roundStatsService = RoundStatsServiceImpl(roundServiceImpl, answerServiceImpl)

  private fun givenRound(formatData: ContentFormatData, vararg answers: AnswerFormatData?) {
    val round = Round(id = ROUND_ID, content = Content(formatData = formatData))
    Mockito.`when`(roundServiceImpl.findById(ROUND_ID)).thenReturn(Optional.of(round))
    val stored = answers.map { Answer(formatData = it) }
    Mockito.`when`(answerServiceImpl.findByRoundId(ROUND_ID)).thenReturn(stored)
    Mockito.`when`(answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(ROUND_ID))
        .thenReturn(stored.filter { !it.hidden })
  }

  private fun choiceStats(vararg answers: AnswerFormatData?): List<Int> {
    givenRound(ChoiceContentFormatData(listOf("A", "B", "C"), null, true), *answers)
    return roundStatsService.getChoiceRoundStatsByRoundId(ROUND_ID).selectionCounts
  }

  private fun wordcloudStats(
      bannedKeywords: List<String>,
      vararg words: List<String>
  ): List<Pair<String, Int>> {
    val formatData = WordcloudContentFormatData()
    bannedKeywords.forEach { formatData.addBannedKeyword(it) }
    givenRound(formatData, *words.map { WordcloudAnswerFormatData(it) }.toTypedArray())
    val stats = roundStatsService.getWordcloudRoundStatsByRoundId(ROUND_ID)
    return stats.words.zip(stats.counts)
  }

  @Test
  fun shouldCountSelectionsPerOptionIncludingUnselectedOnes() {
    Assertions.assertEquals(
        listOf(2, 0, 1),
        choiceStats(ChoiceAnswerFormatData(listOf(0, 2)), ChoiceAnswerFormatData(listOf(0))))
  }

  @Test
  fun shouldIgnoreAbstentionsAndIndexesOutsideTheOptions() {
    Assertions.assertEquals(
        listOf(0, 1, 0),
        choiceStats(null, ChoiceAnswerFormatData(listOf(1, 3, -1)), WordcloudAnswerFormatData()))
  }

  @Test
  fun shouldCountEveryOptionAsZeroWithoutAnswers() {
    Assertions.assertEquals(listOf(0, 0, 0), choiceStats())
  }

  @Test
  fun shouldLeaveHiddenAnswersOutOfChoiceCounts() {
    givenRound(ChoiceContentFormatData(listOf("A", "B", "C"), null, true))
    val visible = Answer(formatData = ChoiceAnswerFormatData(listOf(0)))
    val hidden = Answer(formatData = ChoiceAnswerFormatData(listOf(0, 1)), hidden = true)
    Mockito.`when`(answerServiceImpl.findByRoundId(ROUND_ID)).thenReturn(listOf(visible, hidden))
    Mockito.`when`(answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(ROUND_ID))
        .thenReturn(listOf(visible))

    Assertions.assertEquals(
        listOf(1, 0, 0), roundStatsService.getChoiceRoundStatsByRoundId(ROUND_ID).selectionCounts)
  }

  @Test
  fun shouldMergeWordVariantsDifferingInCaseOrPunctuation() {
    Assertions.assertEquals(
        listOf("Kotlin" to 4),
        wordcloudStats(
            listOf(), listOf("Kotlin"), listOf("kotlin!"), listOf("Kotlin"), listOf(" KOTLIN ")))
  }

  @Test
  fun shouldSortWordsByDescendingCount() {
    Assertions.assertEquals(
        listOf("b" to 3, "a" to 2, "c" to 1),
        wordcloudStats(listOf(), listOf("a", "b"), listOf("c", "b"), listOf("b", "a")))
  }

  @Test
  fun shouldDropBlankAndBannedWords() {
    Assertions.assertEquals(
        listOf("ham" to 1),
        wordcloudStats(listOf("spam"), listOf("spam", " "), listOf(" spam ", "", "ham")))
  }

  @Test
  fun shouldDropVariantsOfBannedWords() {
    Assertions.assertEquals(
        listOf("ham" to 1), wordcloudStats(listOf("Spam"), listOf("spam!", "SPAM", "ham")))
  }

  @Test
  fun shouldRejectStatsForRoundOfOtherFormat() {
    givenRound(ChoiceContentFormatData(listOf("A"), null, false), ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      roundStatsService.getWordcloudRoundStatsByRoundId(ROUND_ID)
    }

    givenRound(WordcloudContentFormatData(), WordcloudAnswerFormatData(listOf("A")))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      roundStatsService.getChoiceRoundStatsByRoundId(ROUND_ID)
    }
  }

  @Test
  fun shouldLeaveHiddenWordsOutOfWordcloud() {
    givenRound(WordcloudContentFormatData())
    val visible = Answer(formatData = WordcloudAnswerFormatData(listOf("ham")))
    val hidden = Answer(formatData = WordcloudAnswerFormatData(listOf("spam")), hidden = true)
    Mockito.`when`(answerServiceImpl.findByRoundId(ROUND_ID)).thenReturn(listOf(visible, hidden))
    Mockito.`when`(answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(ROUND_ID))
        .thenReturn(listOf(visible))

    val stats = roundStatsService.getWordcloudRoundStatsByRoundId(ROUND_ID)

    Assertions.assertEquals(listOf("ham"), stats.words)
    Assertions.assertEquals(listOf(1), stats.counts)
  }

  @Test
  fun shouldRejectUnknownRound() {
    Mockito.`when`(roundServiceImpl.findById(ROUND_ID)).thenReturn(Optional.empty())
    Assertions.assertThrows(RoundNotFoundException::class.java) {
      roundStatsService.getChoiceRoundStatsByRoundId(ROUND_ID)
    }
    Assertions.assertThrows(RoundNotFoundException::class.java) {
      roundStatsService.getWordcloudRoundStatsByRoundId(ROUND_ID)
    }
  }

  private fun numericStats(
      formatData: NumericContentFormatData,
      vararg numbers: Double?
  ): NumericContentRoundStats {
    val answers = numbers.map { it?.let { number -> NumericAnswerFormatData(number) } }
    givenRound(formatData, *answers.toTypedArray())
    return roundStatsService.getNumericRoundStatsByRoundId(ROUND_ID)
  }

  @Test
  fun shouldDescribeNumericAnswersWithPopulationVariance() {
    val stats = numericStats(NumericContentFormatData(0.0, 10.0, 2.0), 5.0, 2.0, 1.0, 2.0)
    Assertions.assertEquals(listOf(1.0, 2.0, 5.0), stats.selectedNumbers)
    Assertions.assertEquals(listOf(1, 2, 1), stats.counts)
    Assertions.assertEquals(2.5, stats.mean)
    Assertions.assertEquals(2.0, stats.median)
    Assertions.assertEquals(2.25, stats.variance)
    Assertions.assertEquals(1.5, stats.standardDeviation)
    Assertions.assertEquals(1.0, stats.minimum)
    Assertions.assertEquals(5.0, stats.maximum)
    Assertions.assertEquals(0.5, stats.correctAnswerFraction)
  }

  @Test
  fun shouldAverageTheMiddleNumbersForEvenCountMedian() {
    Assertions.assertEquals(
        2.5, numericStats(NumericContentFormatData(0.0, 10.0), 4.0, 1.0, 3.0, 2.0).median)
    Assertions.assertEquals(
        3.0, numericStats(NumericContentFormatData(0.0, 10.0), 4.0, 1.0, 3.0).median)
  }

  @Test
  fun shouldDescribeSingleAnswerWithoutSpread() {
    val stats = numericStats(NumericContentFormatData(0.0, 10.0, 6.0, 1.0), 7.0)
    Assertions.assertEquals(7.0, stats.mean)
    Assertions.assertEquals(7.0, stats.median)
    Assertions.assertEquals(0.0, stats.variance)
    Assertions.assertEquals(0.0, stats.standardDeviation)
    Assertions.assertEquals(1.0, stats.correctAnswerFraction)
  }

  @Test
  fun shouldZeroNumericStatsWithoutAnswersAndIgnoreAbstentions() {
    val stats = numericStats(NumericContentFormatData(0.0, 10.0, 5.0), null)
    Assertions.assertEquals(listOf<Double>(), stats.selectedNumbers)
    Assertions.assertEquals(
        listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        listOf(
            stats.mean,
            stats.median,
            stats.variance,
            stats.standardDeviation,
            stats.minimum,
            stats.maximum))
    Assertions.assertEquals(0.0, stats.correctAnswerFraction)
  }

  @Test
  fun shouldOmitCorrectAnswerFractionWithoutCorrectNumber() {
    Assertions.assertNull(
        numericStats(NumericContentFormatData(0.0, 10.0), 1.0).correctAnswerFraction)
  }

  @Test
  fun shouldRejectNumericStatsOfOtherFormat() {
    givenRound(WordcloudContentFormatData())
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      roundStatsService.getNumericRoundStatsByRoundId(ROUND_ID)
    }
  }
}
