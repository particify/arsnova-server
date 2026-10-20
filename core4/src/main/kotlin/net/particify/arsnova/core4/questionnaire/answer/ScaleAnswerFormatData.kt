/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * [rating] is a 0-based index into the options the content's template displays, most positive
 * first, so it is only interpretable together with the content's neutral-option flag.
 */
data class ScaleAnswerFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("rating") val rating: Int) : AnswerFormatData {
  override val format = "SCALE"
}
