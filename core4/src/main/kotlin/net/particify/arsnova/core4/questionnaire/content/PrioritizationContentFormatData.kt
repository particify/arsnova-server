/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.PrioritizationAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

/**
 * A survey format: the participant distributes [ASSIGNABLE_POINTS] over the [options] and there is
 * nothing correct to score, so the interface defaults for correctness and evaluation apply. The
 * budget is a constant because the creator has never been able to set it; format data is jsonb, so
 * turning it into a field later needs no migration.
 */
data class PrioritizationContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("options") val options: List<String>) : ContentFormatData {
  override val format = "PRIORITIZATION"

  override fun withoutRuntimeState() = this

  /**
   * Exposed to the client so it does not need its own copy of the budget the server enforces. It is
   * derived, so it stays out of the persisted format data.
   */
  @get:JsonIgnore
  val assignablePoints: Int
    get() = ASSIGNABLE_POINTS

  override fun validate() {
    if (options.size !in MIN_OPTION_COUNT..MAX_OPTION_COUNT) {
      throw ContentFormatDataInvalidException(
          "A prioritization content must have $MIN_OPTION_COUNT to $MAX_OPTION_COUNT options")
    }
    if (options.any { it.isBlank() || it.length > MAX_OPTION_LENGTH }) {
      throw ContentFormatDataInvalidException(
          "Options must not be blank or exceed $MAX_OPTION_LENGTH characters")
    }
    // Options differing only in spacing or case would look the same to participants.
    if (options.distinctBy { it.trim().lowercase() }.size != options.size) {
      throw ContentFormatDataInvalidException("Options must not repeat")
    }
  }

  /**
   * An answer assigns points by index, so any change to the options, a reordering included, would
   * silently reattribute every answer already given.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? PrioritizationContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a prioritization content")
    if (currentFormatData.options != options && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "Options must not be changed once the content has answers")
    }
  }

  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super<ContentFormatData>.validateAnswer(answerFormatData)
    val assignedPoints = (answerFormatData as PrioritizationAnswerFormatData).assignedPoints
    if (assignedPoints.size != options.size) {
      throw ContentFormatDataInvalidException(
          "An answer must assign points to each of the ${options.size} options")
    }
    if (assignedPoints.any { it !in 0..ASSIGNABLE_POINTS }) {
      throw ContentFormatDataInvalidException(
          "Each option must be assigned between 0 and $ASSIGNABLE_POINTS points")
    }
    if (assignedPoints.sum() != ASSIGNABLE_POINTS) {
      throw ContentFormatDataInvalidException(
          "An answer must assign exactly $ASSIGNABLE_POINTS points in total")
    }
  }

  companion object {
    const val ASSIGNABLE_POINTS = 100
    const val MIN_OPTION_COUNT = 2

    /** Match the limits of the option list in the web client's content form. */
    const val MAX_OPTION_COUNT = 12
    const val MAX_OPTION_LENGTH = 500
  }
}
