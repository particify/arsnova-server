/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import kotlin.reflect.full.cast
import org.springframework.stereotype.Component

/** Resolves the codec of a format by the format's name. */
@Component
class ContentCsvFormatCodecRegistry(codecs: List<ContentCsvFormatCodec<*>>) {
  private val codecsByFormat = codecs.associateBy { it.format }

  init {
    require(codecsByFormat.size == codecs.size) {
      "Content CSV codecs do not carry distinct formats: ${codecs.map { it.format }.sorted()}."
    }
  }

  fun find(format: String): ContentCsvFormatCodec<*>? = codecsByFormat[format]

  /** Fails for a format without a codec rather than exporting it with empty cells. */
  fun encode(formatData: ContentFormatData): ContentCsvFormatCells {
    val codec =
        checkNotNull(find(formatData.format)) { "No CSV codec for format ${formatData.format}" }
    return encode(codec, formatData)
  }

  private fun <T : ContentFormatData> encode(
      codec: ContentCsvFormatCodec<T>,
      formatData: ContentFormatData
  ): ContentCsvFormatCells = codec.encode(codec.type.cast(formatData))
}
