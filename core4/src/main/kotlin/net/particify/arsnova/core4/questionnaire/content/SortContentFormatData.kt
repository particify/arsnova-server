/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.random.Random
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

/**
 * [options] are stored in a random order, so reading them gives the solution away to nobody;
 * [correctOrder] lists their indexes in the sequence the creator declared correct. An answer lists
 * the option indexes in the participant's own order and is correct exactly when it equals
 * [correctOrder]. [correctOrder] is null only in a copy withheld from a reader.
 */
data class SortContentFormatData(
    val options: List<String>,
    val correctOrder: List<Int>? = options.indices.toList(),
) : ContentFormatData {
  override val format = "SORT"

  override fun hasCorrectAnswer(): Boolean = true

  private fun optionIndexes(): List<Int> = options.indices.toList()

  private fun solution(): List<Int> = correctOrder ?: optionIndexes()

  /** The options in the sequence the creator declared correct, as the creator edits them. */
  fun correctlyOrderedOptions(): List<String> = solution().map { options[it] }

  override fun validate() {
    if (options.size < MIN_OPTION_COUNT) {
      throw ContentFormatDataInvalidException(
          "A sort content must have at least $MIN_OPTION_COUNT options")
    }
    if (options.any { it.isBlank() }) {
      throw ContentFormatDataInvalidException("Options must not be blank")
    }
    // Options differing only in spacing or case would look the same to participants.
    if (options.distinctBy { it.trim().lowercase() }.size != options.size) {
      throw ContentFormatDataInvalidException("Options must not repeat")
    }
    if (correctOrder != null && correctOrder.sorted() != optionIndexes()) {
      throw ContentFormatDataInvalidException("Correct order must list each option exactly once")
    }
  }

  /** Keeps the stored order while the creator's sequence is unchanged, so answers stay valid. */
  override fun mergeWith(current: ContentFormatData): ContentFormatData =
      if (current is SortContentFormatData &&
          current.correctlyOrderedOptions() == correctlyOrderedOptions())
          current
      else this

  /**
   * An answer refers to the options by index and is scored against their sequence, so any change to
   * them, a reordering included, would reinterpret every answer already given.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? SortContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a sort content")
    if (currentFormatData.correctlyOrderedOptions() != correctlyOrderedOptions() && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "Options must not be changed once the content has answers")
    }
  }

  override fun withoutSolution() = copy(correctOrder = null)

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val selectedChoiceIndexes = (answerFormatData as SortAnswerFormatData).selectedChoiceIndexes
    if (selectedChoiceIndexes.sorted() != optionIndexes()) {
      throw ContentFormatDataInvalidException(
          "An answer must order each of the ${options.size} options exactly once")
    }
  }

  override fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState {
    val selectedChoiceIndexes = (answerFormatData as SortAnswerFormatData).selectedChoiceIndexes
    return if (selectedChoiceIndexes == solution()) AnswerResultState.CORRECT
    else AnswerResultState.WRONG
  }

  companion object {
    const val MIN_OPTION_COUNT = 2

    /** Stored before the correct order was kept apart from the options, which held it instead. */
    @JvmStatic
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    fun fromJson(
        @JsonProperty("options") options: List<String>,
        @JsonProperty("correctOrder") correctOrder: List<Int>?
    ) = SortContentFormatData(options, correctOrder ?: options.indices.toList())

    /** Stores options given in their correct sequence in a random order. */
    fun shuffled(
        correctlyOrderedOptions: List<String>,
        random: Random = Random.Default
    ): SortContentFormatData {
      // The correct position of the option stored at each index.
      val correctPositions = correctlyOrderedOptions.indices.shuffled(random)
      return SortContentFormatData(
          correctPositions.map { correctlyOrderedOptions[it] },
          correctlyOrderedOptions.indices.map { correctPositions.indexOf(it) })
    }
  }
}
