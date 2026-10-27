/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import org.springframework.stereotype.Component

@Component
class BinaryCsvCodec : ContentCsvFormatCodec<BinaryContentFormatData> {
  override val format = "BINARY"
  override val type = BinaryContentFormatData::class

  override fun encode(formatData: BinaryContentFormatData) =
      ContentCsvFormatCells(
          options = LABELS,
          correctOptions =
              listOfNotNull(formatData.correctAnswer?.let { LABELS[if (it) 0 else 1] }))

  override fun decode(cells: ContentCsvFormatCells): BinaryContentFormatData {
    if (cells.options.size != LABELS.size) {
      throw invalid("A binary content must have exactly two options, yes first")
    }
    val correctIndexes = correctIndexes(cells)
    if (correctIndexes.size > 1) throw invalid("A binary content has at most one correct option")
    return BinaryContentFormatData(correctIndexes.singleOrNull()?.let { it == 0 })
  }

  companion object {
    /**
     * Binary contents do not store their labels, so these stand in for them. Only their position
     * matters when the file is imported again: the first one is the yes option.
     */
    val LABELS = listOf("Yes", "No")
  }
}
