/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ShortAnswerAnswerFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("body") val body: String) : AnswerFormatData {
  override val format = "SHORT_ANSWER"
}
