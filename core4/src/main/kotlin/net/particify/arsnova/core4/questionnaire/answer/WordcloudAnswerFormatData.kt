/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

data class WordcloudAnswerFormatData(val words: List<String>) : AnswerFormatData {
  override val format = "WORDCLOUD"

  constructor() : this(listOf())
}
