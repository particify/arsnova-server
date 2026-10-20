/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * Raw per-value counts plus descriptive statistics of a numeric round. [selectedNumbers] and
 * [counts] are parallel and ascending by value; grouping them into chart buckets is left to the
 * client. [correctAnswerFraction] is null for a content without a correct number. A round without
 * answers reports 0.0 for [minimum], [maximum], [mean] and [median] rather than leaving them out,
 * so only its empty [counts] tell it apart from a round whose answers all selected 0.
 */
data class NumericContentRoundStats(
    val selectedNumbers: List<Double>,
    val counts: List<Int>,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val variance: Double,
    val minimum: Double,
    val maximum: Double,
    val correctAnswerFraction: Double?,
) {
  /** The correct answer fraction tells whether the correct number is among the selected ones. */
  fun withoutSolution(): NumericContentRoundStats = copy(correctAnswerFraction = null)
}
