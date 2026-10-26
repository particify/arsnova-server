/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import org.springframework.stereotype.Component

@Component
class TextCsvCodec : ContentCsvFormatCodec<TextContentFormatData> {
  override val format = "TEXT"
  override val type = TextContentFormatData::class

  override fun encode(formatData: TextContentFormatData) = ContentCsvFormatCells()

  override fun decode(cells: ContentCsvFormatCells) = TextContentFormatData(false)
}
