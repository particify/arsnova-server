/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import org.springframework.stereotype.Component

@Component
class ChoiceCsvCodec : ContentCsvFormatCodec<ChoiceContentFormatData> {
  override val format = "CHOICE"
  override val type = ChoiceContentFormatData::class

  override fun encode(formatData: ChoiceContentFormatData): ContentCsvFormatCells {
    val correctIndexes = formatData.correctOptionIndexes.orEmpty()
    return ContentCsvFormatCells(
        options = formatData.options,
        correctOptions = formatData.options.filterIndexed { index, _ -> index in correctIndexes },
        multiple = formatData.multiple == true,
    )
  }

  override fun decode(cells: ContentCsvFormatCells) = decodeChoice(cells)
}
