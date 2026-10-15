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
import net.particify.arsnova.core4.questionnaire.answer.PrioritizationAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
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

  /**
   * Sums the points each option was assigned. A missing entry counts as zero, so an answer with an
   * empty list contributes nothing and the result is as long as the option list either way.
   */
  @Transactional
  fun getPrioritizationRoundStatsByRoundId(roundId: UUID): PrioritizationContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val formatData =
        round.content?.formatData as? PrioritizationContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a prioritization content")
    val assignedPoints =
        answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId).mapNotNull {
          (it.formatData as? PrioritizationAnswerFormatData)?.assignedPoints
        }
    return PrioritizationContentRoundStats(
        formatData.options.indices.map { index ->
          assignedPoints.sumOf { points -> points.getOrElse(index) { 0 } }
        })
  }

  @Transactional
  fun getScaleRoundStatsByRoundId(roundId: UUID): ScaleContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val formatData =
        round.content?.formatData as? ScaleContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a scale content")
    val countsByRating =
        answerServiceImpl
            .findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)
            .mapNotNull { (it.formatData as? ScaleAnswerFormatData)?.rating }
            .groupingBy { it }
            .eachCount()
    return ScaleContentRoundStats(
        (0 until formatData.optionCount()).map { countsByRating.getOrDefault(it, 0) })
  }

  /**
   * Labels a group with the matching correct term only for a [reader] who may see the solution,
   * since the creator's spelling of a term would give it away. The reader is passed in rather than
   * taken from the security context because subscription emissions run without one.
   */
  @Transactional
  fun getShortAnswerRoundStatsByRoundId(
      roundId: UUID,
      reader: Authentication?
  ): ShortAnswerContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    val stats = shortAnswerStats(round)
    val maySeeSolution = solutionVisibility.maySeeSolution(round.content!!, reader)
    return if (maySeeSolution) stats else stats.withoutSolution()
  }

  /**
   * Labels groups with the matching correct term whoever reads them, for subscriptions which share
   * one computation among all of their subscribers and relabel them per subscriber.
   */
  @Transactional
  fun getShortAnswerRoundStatsWithSolutionByRoundId(roundId: UUID): ShortAnswerContentRoundStats =
      shortAnswerStats(
          roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId))

  private fun shortAnswerStats(round: Round): ShortAnswerContentRoundStats {
    val formatData =
        round.content?.formatData as? ShortAnswerContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a short answer content")
    val bodies =
        answerServiceImpl.findByRoundIdAndHiddenFalseOrderByIdAsc(round.id!!).mapNotNull {
          (it.formatData as? ShortAnswerAnswerFormatData)?.body
        }
    val aggregated = aggregateTexts(bodies, formatData.bannedKeywords, formatData.correctTerms)
    return ShortAnswerContentRoundStats(
        aggregated.map { it.text },
        aggregated.map { it.count },
        aggregated.map { it.mostCommonVariant })
  }

  @Transactional
  fun getSortRoundStatsByRoundId(roundId: UUID): SortContentRoundStats {
    val round = roundServiceImpl.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    if (round.content?.formatData !is SortContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a sort content")
    }
    val combinations =
        answerServiceImpl
            .findByRoundIdAndHiddenFalseOrderByIdAsc(roundId)
            .mapNotNull { (it.formatData as? SortAnswerFormatData)?.selectedChoiceIndexes }
            .groupingBy { it }
            .eachCount()
            .map { (selectedChoiceIndexes, count) -> SortCombination(selectedChoiceIndexes, count) }
            .sortedByDescending { it.count }
    return SortContentRoundStats(combinations)
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

  /**
   * Groups texts by their normalized form, which banned keywords are matched by too, and labels
   * each group with its most common variant, or with the matching [preferredTerms] entry's own
   * spelling where there is one. The client highlights correct answers by exact string match, so
   * that relabelling is what makes a correct term recognizable even when nobody spelled it the way
   * the creator did.
   */
  private fun aggregateTexts(
      texts: List<String>,
      bannedKeywords: List<String>,
      preferredTerms: List<String> = emptyList()
  ): List<TextCount> {
    val normalizedBannedKeywords = bannedKeywords.map { normalizeAnswerText(it) }.toSet()
    val preferredTermsByNormalizedForm = preferredTerms.associateBy { normalizeAnswerText(it) }
    return texts
        .map { it.trim() }
        .filter { it.isNotBlank() && normalizeAnswerText(it) !in normalizedBannedKeywords }
        .groupingBy { it }
        .eachCount()
        .entries
        .groupBy { normalizeAnswerText(it.key) }
        .map { (normalizedForm, variants) ->
          val mostCommonVariant = variants.maxBy { it.value }.key
          TextCount(
              text = preferredTermsByNormalizedForm[normalizedForm] ?: mostCommonVariant,
              count = variants.sumOf { it.value },
              mostCommonVariant = mostCommonVariant)
        }
        .sortedByDescending { it.count }
  }

  private data class TextCount(
      val text: String,
      val count: Int,
      val mostCommonVariant: String,
  )
}
