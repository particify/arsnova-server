/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.springframework.stereotype.Component

/**
 * Reads records in the cell encodings of the v3 export. Each record is checked as thoroughly as
 * creating the content would check it, so that an import can tell the valid records from the
 * invalid ones before it creates any content.
 */
@Component
class ContentCsvDecoder(private val codecs: ContentCsvFormatCodecRegistry) {
  /** Throws [ContentCsvRecordException] naming why [record] cannot become a content. */
  fun decode(record: ContentCsvRecord): DecodedContent {
    val codec =
        record.format?.trim()?.uppercase()?.let(codecs::find)
            ?: throw ContentCsvRecordException(
                ContentCsvImportErrorReason.UNKNOWN_FORMAT, "Unknown format: ${record.format}")
    val body =
        record.body?.takeIf { it.isNotBlank() }
            ?: throw ContentCsvRecordException(
                ContentCsvImportErrorReason.MISSING_BODY, "Body must not be blank")
    val duration = decodeDuration(record.duration)
    val formatData = validated(codec.decode(formatCells(record)))
    // Matches what creating a content of a format that cannot be answered stores.
    if (!formatData.answerable()) return DecodedContent(body, formatData, null, false)
    return DecodedContent(body, formatData, duration, record.abstentionsAllowed ?: true)
  }

  private fun formatCells(record: ContentCsvRecord) =
      ContentCsvFormatCells(
          additionalText = record.additionalText,
          options = record.options,
          correctOptions = record.correctOptions,
          multiple = record.multiple == true,
      )

  /** v3 exports a content without a time limit with a duration of zero. */
  private fun decodeDuration(duration: Int?): Int? {
    if (duration != null && duration !in 0..Content.MAX_DURATION.toInt()) {
      throw invalid("Duration must be between 0 and ${Content.MAX_DURATION} seconds")
    }
    return duration?.takeIf { it > 0 }
  }

  private fun validated(formatData: ContentFormatData): ContentFormatData {
    try {
      formatData.validate()
    } catch (e: ContentFormatDataInvalidException) {
      throw invalid(e.message ?: "Invalid format data", e)
    }
    return formatData
  }

  private fun invalid(message: String, cause: Throwable? = null) =
      ContentCsvRecordException(ContentCsvImportErrorReason.INVALID_FORMAT_DATA, message, cause)
}

data class DecodedContent(
    val body: String,
    val formatData: ContentFormatData,
    val duration: Int?,
    val abstentionsAllowed: Boolean,
)
