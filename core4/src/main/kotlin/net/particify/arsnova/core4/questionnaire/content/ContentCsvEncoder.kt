/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.springframework.stereotype.Component

/** Writes contents in the cell encodings of the v3 export, which [ContentCsvDecoder] reads. */
@Component
class ContentCsvEncoder(private val codecs: ContentCsvFormatCodecRegistry) {
  fun encode(content: Content): ContentCsvRecord {
    val formatData = content.formatData!!
    val cells = codecs.encode(formatData)
    return ContentCsvRecord(
        format = formatData.format,
        body = content.body,
        additionalText = cells.additionalText,
        options = cells.options,
        correctOptions = cells.correctOptions,
        multiple = cells.multiple,
        abstentionsAllowed = content.abstentionsAllowed,
        duration = content.duration ?: 0,
    )
  }
}
