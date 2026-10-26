/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import org.springframework.stereotype.Component

@Component
class WordcloudCsvCodec : ContentCsvFormatCodec<WordcloudContentFormatData> {
  override val format = "WORDCLOUD"
  override val type = WordcloudContentFormatData::class

  override fun encode(formatData: WordcloudContentFormatData) =
      ContentCsvFormatCells(options = listOf(formatData.maxAnswers.toString()))

  override fun decode(cells: ContentCsvFormatCells): WordcloudContentFormatData {
    val maxAnswers =
        cells.options.singleOrNull()?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            ?: throw invalid("A wordcloud content needs the maximum number of words as option")
    return WordcloudContentFormatData(maxAnswers)
  }
}
