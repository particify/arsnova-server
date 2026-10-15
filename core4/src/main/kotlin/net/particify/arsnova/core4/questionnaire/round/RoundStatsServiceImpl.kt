/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import jakarta.transaction.Transactional
import java.util.*
import kotlin.Int
import kotlin.String
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.content.normalizeAnswerText
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class RoundStatsServiceImpl(
    private val roundServiceImpl: RoundServiceImpl,
    private val answerServiceImpl: AnswerServiceImpl
) {
  @Transactional
  fun getChoiceRoundStatsByRoundId(roundId: UUID): ChoiceContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val answers = answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)
    val optionCount = (round.content!!.formatData as? ChoiceContentFormatData)?.options?.size ?: 0

    val countMap =
        answers
            .mapNotNull { it.formatData as? ChoiceAnswerFormatData }
            .flatMap { it.selectedChoiceIndexes }
            .groupingBy { it }
            .eachCount()
    val optionCounts = (0 until optionCount).map { countMap.getOrDefault(it, 0) }
    return ChoiceContentRoundStats(optionCounts)
  }

  @Transactional
  fun getWordcloudRoundStatsByRoundId(roundId: UUID): WordcloudContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val answers = answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)
    val formatData =
        round.content?.formatData as? WordcloudContentFormatData
            ?: return WordcloudContentRoundStats(listOf(), listOf())

    val aggregated =
        aggregateTexts(
            answers.mapNotNull { it.formatData as? WordcloudAnswerFormatData }.flatMap { it.words },
            formatData.bannedKeywords)
    return WordcloudContentRoundStats(aggregated.map { it.text }, aggregated.map { it.count })
  }

  /** Keywords are banned by their normalized form, the same form texts are grouped by. */
  private fun aggregateTexts(texts: List<String>, bannedKeywords: List<String>): List<TextCount> {
    val normalizedBannedKeywords = bannedKeywords.map { normalizeAnswerText(it) }.toSet()
    return texts
        .map { it.trim() }
        .filter { it.isNotBlank() && normalizeAnswerText(it) !in normalizedBannedKeywords }
        .groupingBy { it }
        .eachCount()
        .entries
        .groupBy { normalizeAnswerText(it.key) }
        .map { (_, variants) ->
          TextCount(text = variants.maxBy { it.value }.key, count = variants.sumOf { it.value })
        }
        .sortedByDescending { it.count }
  }

  private data class TextCount(
      val text: String,
      val count: Int,
  )
}
