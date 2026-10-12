/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import kotlin.math.roundToInt
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

data class ChoiceContentFormatData(
    val options: List<String>,
    val correctOptionIndexes: List<Int>?,
    val multiple: Boolean?
) : ContentFormatData {
  override val format = "CHOICE"

  override fun supportsRounds() = true

  override fun withoutSolution() = copy(correctOptionIndexes = null)

  override fun validate() {
    if (options.size < MIN_OPTION_COUNT) {
      throw ContentFormatDataInvalidException(
          "A choice content needs at least $MIN_OPTION_COUNT options")
    }
    if (options.any { it.isBlank() }) {
      throw ContentFormatDataInvalidException("Options must not be blank")
    }
    val correctIndexes = correctOptionIndexes.orEmpty()
    if (correctIndexes.any { it !in options.indices }) {
      throw ContentFormatDataInvalidException(
          "Correct options must be between 0 and ${options.size - 1} for this choice content")
    }
    if (correctIndexes.distinct().size != correctIndexes.size) {
      throw ContentFormatDataInvalidException("An option must not be marked correct twice")
    }
  }

  /** An update of another format would reinterpret the content's rounds and answers. */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    if (current !is ChoiceContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a choice content")
    }
  }

  /** An empty index list marks no option as correct, just like a missing one. */
  override fun hasCorrectAnswer(): Boolean {
    return !this.correctOptionIndexes.isNullOrEmpty()
  }

  /**
   * Scoring adds up the selected options one by one, so each may be selected at most once. Only a
   * content marked [multiple] accepts more than one selection.
   */
  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val selectedChoiceIndexes = (answerFormatData as ChoiceAnswerFormatData).selectedChoiceIndexes
    if (selectedChoiceIndexes.any { it !in options.indices }) {
      throw ContentFormatDataInvalidException(
          "Selected options must be between 0 and ${options.size - 1} for this choice content")
    }
    if (selectedChoiceIndexes.distinct().size != selectedChoiceIndexes.size) {
      throw ContentFormatDataInvalidException("An option must not be selected more than once")
    }
    if (multiple != true && selectedChoiceIndexes.size > 1) {
      throw ContentFormatDataInvalidException("Only one option may be selected for this content")
    }
  }

  override fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState {
    if (correctOptionIndexes.isNullOrEmpty()) return AnswerResultState.NEUTRAL
    return if (calculateAchievedRatio(answerFormatData) > CORRECTNESS_THRESHOLD)
        AnswerResultState.CORRECT
    else AnswerResultState.WRONG
  }

  override fun calculateAchievedPoints(answerFormatData: AnswerFormatData, basePoints: Int): Int =
      (calculateAchievedRatio(answerFormatData) * basePoints).roundToInt()

  /**
   * Every correctly selected option earns and every wrongly selected one forfeits the same share of
   * the content's worth, so a partially correct answer is worth part of it. Selecting more wrong
   * options than right ones is not punished beyond scoring nothing.
   *
   * Stored answers are scored again long after they were given, and a content's format can be
   * changed while it holds answers, so an answer of another format scores nothing rather than
   * failing the whole summary it is part of.
   */
  private fun calculateAchievedRatio(answerFormatData: AnswerFormatData): Double {
    val correctIndexes = correctOptionIndexes
    if (correctIndexes.isNullOrEmpty()) return 0.0
    val choiceAnswerFormatData = answerFormatData as? ChoiceAnswerFormatData ?: return 0.0
    val selectedChoiceIndexes = choiceAnswerFormatData.selectedChoiceIndexes
    val ratioPerOption = 1.0 / correctIndexes.size
    return selectedChoiceIndexes
        .sumOf { if (correctIndexes.contains(it)) ratioPerOption else -ratioPerOption }
        .coerceAtLeast(0.0)
  }

  constructor() : this(listOf(), null, false)

  companion object {
    const val MIN_OPTION_COUNT = 2

    /**
     * Summing the share of each option leaves a fully correct answer just short of the whole for
     * option counts which cannot be represented exactly, so correctness is decided against a
     * threshold rather than an exact value.
     */
    private const val CORRECTNESS_THRESHOLD = 0.999
  }
}

data class AnswerOption(val label: String, val labelRendered: String?)
