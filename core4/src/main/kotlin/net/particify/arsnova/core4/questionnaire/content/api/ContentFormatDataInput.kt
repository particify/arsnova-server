/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import org.springframework.data.web.ProjectedPayload

data class ContentFormatDataInput(
    val choice: ChoiceContentFormatDataInput?,
    val numeric: NumericContentFormatDataInput?,
    val scale: ScaleContentFormatDataInput?,
    val text: TextContentFormatDataInput?
) {

  fun toFormatData(): ContentFormatData {
    return when {
      choice != null ->
          ChoiceContentFormatData(choice.choices, choice.correctOptionIndexes, choice.multiple)
      numeric != null ->
          NumericContentFormatData(
              numeric.minNumber, numeric.maxNumber, numeric.correctNumber, numeric.tolerance)
      scale != null -> ScaleContentFormatData(scale.template, scale.neutralOption)
      text != null -> TextContentFormatData(text.answersPublished)
      else -> error("No FormatData available.")
    }
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
  interface ScaleContentFormatDataInput {
    val template: ScaleContentFormatData.Template
    val neutralOption: Boolean
  }

  @ProjectedPayload
  interface TextContentFormatDataInput {
    val answersPublished: Boolean
  }
}
