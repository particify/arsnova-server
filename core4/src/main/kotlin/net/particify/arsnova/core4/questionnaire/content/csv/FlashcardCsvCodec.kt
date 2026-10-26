/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import org.springframework.stereotype.Component

@Component
class FlashcardCsvCodec : ContentCsvFormatCodec<FlashcardContentFormatData> {
  override val format = "FLASHCARD"
  override val type = FlashcardContentFormatData::class

  override fun encode(formatData: FlashcardContentFormatData) =
      ContentCsvFormatCells(additionalText = formatData.answer)

  override fun decode(cells: ContentCsvFormatCells) =
      FlashcardContentFormatData(cells.additionalText.orEmpty())
}
