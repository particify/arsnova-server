/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import org.springframework.stereotype.Component

@Component
class ScaleCsvCodec : ContentCsvFormatCodec<ScaleContentFormatData> {
  override val format = "SCALE"
  override val type = ScaleContentFormatData::class

  override fun encode(formatData: ScaleContentFormatData) =
      ContentCsvFormatCells(
          options = listOf("${formatData.template.name} (${formatData.optionCount()})"))

  /**
   * Like v3, a scale content whose options do not name a template is read as a choice content,
   * which is what scale contents were before they had templates.
   */
  override fun decode(cells: ContentCsvFormatCells): ContentFormatData {
    val match = cells.options.singleOrNull()?.let { TEMPLATE.matchEntire(it.trim()) }
    val templateName = match?.groupValues?.get(1)
    val template = ScaleContentFormatData.Template.entries.find { it.name == templateName }
    if (match == null || template == null) return decodeChoice(cells)
    val optionCount = match.groupValues[2].toInt()
    return ScaleContentFormatData(
        template, optionCount == ScaleContentFormatData.OPTION_COUNT_WITH_NEUTRAL)
  }

  companion object {
    private val TEMPLATE = Regex("""([A-Z_]+) \(([45])\)""")
  }
}
