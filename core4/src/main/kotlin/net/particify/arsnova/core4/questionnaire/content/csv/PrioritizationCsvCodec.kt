/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
import org.springframework.stereotype.Component

@Component
class PrioritizationCsvCodec : ContentCsvFormatCodec<PrioritizationContentFormatData> {
  override val format = "PRIORITIZATION"
  override val type = PrioritizationContentFormatData::class

  override fun encode(formatData: PrioritizationContentFormatData) =
      ContentCsvFormatCells(options = formatData.options)

  override fun decode(cells: ContentCsvFormatCells) = PrioritizationContentFormatData(cells.options)
}
