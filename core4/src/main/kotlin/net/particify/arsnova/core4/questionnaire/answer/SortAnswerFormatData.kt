/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/** [selectedChoiceIndexes] are the content's option indexes in the order the participant chose. */
data class SortAnswerFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("selectedChoiceIndexes") val selectedChoiceIndexes: List<Int>) :
    AnswerFormatData {
  override val format = "SORT"
}
