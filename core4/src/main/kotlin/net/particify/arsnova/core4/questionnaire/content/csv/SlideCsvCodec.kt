/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData
import org.springframework.stereotype.Component

@Component
class SlideCsvCodec : ContentCsvFormatCodec<SlideContentFormatData> {
  override val format = "SLIDE"
  override val type = SlideContentFormatData::class

  override fun encode(formatData: SlideContentFormatData) = ContentCsvFormatCells()

  override fun decode(cells: ContentCsvFormatCells) = SlideContentFormatData()
}
