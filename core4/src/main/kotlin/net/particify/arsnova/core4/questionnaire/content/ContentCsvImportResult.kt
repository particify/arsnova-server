/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

/** Lines are numbered by record, counting from the first one after the header. */
data class ContentCsvImportResult(
    val totalLines: Int,
    val importedLines: Int,
    val errorLines: List<ContentCsvImportError>,
)

data class ContentCsvImportError(
    val line: Int,
    val reason: ContentCsvImportErrorReason,
    val message: String?,
)

enum class ContentCsvImportErrorReason {
  INCOMPATIBLE_FORMAT,
  INVALID_FORMAT_DATA,
  MALFORMED_RECORD,
  MISSING_BODY,
  UNKNOWN_FORMAT,
}

/** Rejects a single record of an import, which leaves the other records unaffected. */
class ContentCsvRecordException(
    val reason: ContentCsvImportErrorReason,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
