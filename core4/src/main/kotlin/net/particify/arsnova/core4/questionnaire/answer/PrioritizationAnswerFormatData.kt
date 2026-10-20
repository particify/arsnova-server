/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/** [assignedPoints] are the points the participant gave the content's options, by index. */
data class PrioritizationAnswerFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("assignedPoints") val assignedPoints: List<Int>) : AnswerFormatData {
  override val format = "PRIORITIZATION"
}
