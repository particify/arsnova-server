/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import org.springframework.stereotype.Component

/** The options are stored shuffled, but the file lists them in the correct order. */
@Component
class SortCsvCodec : ContentCsvFormatCodec<SortContentFormatData> {
  override val format = "SORT"
  override val type = SortContentFormatData::class

  override fun encode(formatData: SortContentFormatData) =
      formatData.correctlyOrderedOptions().let {
        ContentCsvFormatCells(options = it, correctOptions = it)
      }

  override fun decode(cells: ContentCsvFormatCells) = SortContentFormatData.shuffled(cells.options)
}
