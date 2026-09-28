/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

data class ChoiceAnswerFormatData(val selectedChoiceIndexes: List<Int>) : AnswerFormatData {
  override val format = "CHOICE"

  constructor() : this(listOf())
}
