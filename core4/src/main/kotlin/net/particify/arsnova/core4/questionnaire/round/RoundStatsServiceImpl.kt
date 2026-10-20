/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import jakarta.transaction.Transactional
import java.util.*
import kotlin.Int
import kotlin.String
import kotlin.math.pow
import kotlin.math.sqrt
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.content.normalizeAnswerText
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class RoundStatsServiceImpl(
    private val roundServiceImpl: RoundServiceImpl,
    private val answerServiceImpl: AnswerServiceImpl,
    private val solutionVisibility: SolutionVisibility
) {
  @Transactional
  fun getChoiceRoundStatsByRoundId(roundId: UUID): ChoiceContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val formatData =
        round.content?.formatData as? ChoiceContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a choice content")
    val answers = answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)
    val optionCount = formatData.options.size

    val countMap =
        answers
            .mapNotNull { it.formatData as? ChoiceAnswerFormatData }
            .flatMap { it.selectedChoiceIndexes }
            .groupingBy { it }
            .eachCount()
    val optionCounts = (0 until optionCount).map { countMap.getOrDefault(it, 0) }
    return ChoiceContentRoundStats(optionCounts)
  }

  /**
   * Leaves out the correct answer fraction for a [reader] who may not see the solution, since it
   * tells whether the correct number is among the selected ones. The reader is passed in rather
   * than taken from the security context because subscription emissions run without one.
   */
  @Transactional
  fun getNumericRoundStatsByRoundId(
      roundId: UUID,
      reader: Authentication?
  ): NumericContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val stats = numericStats(round)
    val maySeeSolution = solutionVisibility.maySeeSolution(round.content!!, reader)
    return if (maySeeSolution) stats else stats.withoutSolution()
  }

  /**
   * Includes the correct answer fraction whoever reads it, for subscriptions which share one
   * computation among all of their subscribers and leave it out per subscriber.
   */
  @Transactional
  fun getNumericRoundStatsWithSolutionByRoundId(roundId: UUID): NumericContentRoundStats =
      numericStats(
          roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId))

  private fun numericStats(round: Round): NumericContentRoundStats {
    val formatData =
        round.content?.formatData as? NumericContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a numeric content")
    val numbers =
        answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(round.id!!).mapNotNull {
          (it.formatData as? NumericAnswerFormatData)?.selectedNumber
        }
    val countsByNumber = numbers.groupingBy { it }.eachCount().toSortedMap()
    val mean = mean(numbers)
    val variance = variance(numbers, mean)
    return NumericContentRoundStats(
        selectedNumbers = countsByNumber.keys.toList(),
        counts = countsByNumber.values.toList(),
        mean = mean,
        median = median(numbers),
        standardDeviation = sqrt(variance),
        variance = variance,
        minimum = numbers.minOrNull() ?: 0.0,
        maximum = numbers.maxOrNull() ?: 0.0,
        correctAnswerFraction = correctAnswerFraction(numbers, formatData),
    )
  }

  private fun mean(numbers: List<Double>): Double =
      if (numbers.isEmpty()) 0.0 else numbers.average()

  private fun median(numbers: List<Double>): Double {
    if (numbers.isEmpty()) return 0.0
    val sorted = numbers.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
  }

  private fun variance(numbers: List<Double>, mean: Double): Double =
      if (numbers.isEmpty()) 0.0 else numbers.sumOf { (it - mean).pow(2) } / numbers.size

  private fun correctAnswerFraction(
      numbers: List<Double>,
      formatData: NumericContentFormatData
  ): Double? {
    if (!formatData.hasCorrectAnswer()) return null
    if (numbers.isEmpty()) return 0.0
    return numbers.count { formatData.isCorrect(it) }.toDouble() / numbers.size
  }

  @Transactional
  fun getWordcloudRoundStatsByRoundId(roundId: UUID): WordcloudContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val formatData =
        round.content?.formatData as? WordcloudContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a wordcloud content")
    val answers = answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)

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
