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
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
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
    Mockito.`when`(answerServiceImpl.findByRoundId(ROUND_ID))
        .thenReturn(answers.map { Answer(formatData = it) })
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
  fun shouldReturnEmptyStatsForRoundOfOtherFormat() {
    givenRound(ChoiceContentFormatData(listOf("A"), null, false), ChoiceAnswerFormatData(listOf(0)))
    val choiceStats = roundStatsService.getWordcloudRoundStatsByRoundId(ROUND_ID)
    Assertions.assertEquals(listOf<String>(), choiceStats.words)
    Assertions.assertEquals(listOf<Int>(), choiceStats.counts)

    givenRound(WordcloudContentFormatData(), WordcloudAnswerFormatData(listOf("A")))
    Assertions.assertEquals(
        listOf<Int>(), roundStatsService.getChoiceRoundStatsByRoundId(ROUND_ID).selectionCounts)
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
}
