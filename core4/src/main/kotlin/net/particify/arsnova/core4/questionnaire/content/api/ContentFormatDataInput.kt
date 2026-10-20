/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import org.springframework.data.web.ProjectedPayload

data class ContentFormatDataInput(
    val binary: BinaryContentFormatDataInput?,
    val choice: ChoiceContentFormatDataInput?,
    val numeric: NumericContentFormatDataInput?,
    val prioritization: PrioritizationContentFormatDataInput?,
    val scale: ScaleContentFormatDataInput?,
    val shortAnswer: ShortAnswerContentFormatDataInput?,
    val sort: SortContentFormatDataInput?,
    val text: TextContentFormatDataInput?
) {

  fun toFormatData(): ContentFormatData {
    return when {
      binary != null -> BinaryContentFormatData(binary.correctAnswer)
      choice != null ->
          ChoiceContentFormatData(choice.choices, choice.correctOptionIndexes, choice.multiple)
      numeric != null ->
          NumericContentFormatData(
              numeric.minNumber, numeric.maxNumber, numeric.correctNumber, numeric.tolerance)
      prioritization != null -> PrioritizationContentFormatData(prioritization.options)
      scale != null -> ScaleContentFormatData(scale.template, scale.neutralOption)
      shortAnswer != null -> ShortAnswerContentFormatData(shortAnswer.correctTerms)
      sort != null -> SortContentFormatData.shuffled(sort.options)
      text != null -> TextContentFormatData(text.answersPublished)
      else -> error("No FormatData available.")
    }
  }

  @ProjectedPayload
  interface BinaryContentFormatDataInput {
    val correctAnswer: Boolean?
  }

  @ProjectedPayload
  interface ChoiceContentFormatDataInput {
    val choices: List<String>
    val correctOptionIndexes: List<Int>
    val multiple: Boolean
  }

  @ProjectedPayload
  interface NumericContentFormatDataInput {
    val minNumber: Double
    val maxNumber: Double
    val correctNumber: Double?
    val tolerance: Double
  }

  @ProjectedPayload
  interface PrioritizationContentFormatDataInput {
    val options: List<String>
  }

  @ProjectedPayload
  interface ScaleContentFormatDataInput {
    val template: ScaleContentFormatData.Template
    val neutralOption: Boolean
  }

  @ProjectedPayload
  interface ShortAnswerContentFormatDataInput {
    val correctTerms: List<String>
  }

  @ProjectedPayload
  interface SortContentFormatDataInput {
    val options: List<String>
  }

  @ProjectedPayload
  interface TextContentFormatDataInput {
    val answersPublished: Boolean
  }
}
