/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import org.springframework.stereotype.Component

@Component
class ShortAnswerCsvCodec : ContentCsvFormatCodec<ShortAnswerContentFormatData> {
  override val format = "SHORT_ANSWER"
  override val type = ShortAnswerContentFormatData::class

  override fun encode(formatData: ShortAnswerContentFormatData) =
      ContentCsvFormatCells(
          options = formatData.correctTerms, correctOptions = formatData.correctTerms)

  /** v3 lists the terms as options, as correct options or as both. */
  override fun decode(cells: ContentCsvFormatCells): ShortAnswerContentFormatData {
    val terms =
        when {
          cells.options.isEmpty() -> cells.correctOptions
          cells.correctOptions.isEmpty() || cells.correctOptions == cells.options -> cells.options
          else -> throw invalid("Options and correct options of a short answer content differ")
        }
    return ShortAnswerContentFormatData(terms.distinct())
  }
}
