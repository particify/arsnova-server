/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import org.springframework.data.web.ProjectedPayload

data class ContentFormatDataInput(
    val choice: ChoiceContentFormatDataInput?,
    val text: TextContentFormatDataInput?
) {

  fun toFormatData(): ContentFormatData {
    return when {
      choice != null ->
          ChoiceContentFormatData(choice.choices, choice.correctOptionIndexes, choice.multiple)
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
  interface TextContentFormatDataInput {
    val answersPublished: Boolean
  }
}
