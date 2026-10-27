/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

/**
 * A content in the column layout of the v3 export, so that files exported by v3 can be imported.
 * List cells hold one entry per line.
 */
data class ContentCsvRecord(
    val format: String?,
    val body: String?,
    val additionalText: String? = null,
    val options: List<String> = listOf(),
    val correctOptions: List<String> = listOf(),
    val multiple: Boolean? = null,
    val abstentionsAllowed: Boolean? = null,
    val duration: Int? = null,
) {
  fun toCells(): List<String?> =
      listOf(
          format,
          body,
          additionalText,
          options.joinToString(LIST_SEPARATOR),
          correctOptions.joinToString(LIST_SEPARATOR),
          multiple?.toString(),
          abstentionsAllowed?.toString(),
          duration?.toString(),
      )

  companion object {
    private const val LIST_SEPARATOR = "\n"

    /** In the order of the v3 export. */
    val COLUMNS =
        listOf(
            "format",
            "body",
            "additionalText",
            "options",
            "correctOptions",
            "multiple",
            "abstentionsAllowed",
            "duration",
        )

    /** The name of [COLUMNS] which [name] matches regardless of case, if any. */
    fun columnOf(name: String?): String? =
        name?.trim()?.let { trimmed -> COLUMNS.find { it.equals(trimmed, ignoreCase = true) } }

    /** Cells of columns missing from [cells] are taken as empty. */
    fun fromCells(cells: Map<String, String?>): ContentCsvRecord =
        ContentCsvRecord(
            format = cells["format"],
            body = cells["body"],
            additionalText = cells["additionalText"],
            options = parseList(cells["options"]),
            correctOptions = parseList(cells["correctOptions"]),
            multiple = parseBoolean(cells, "multiple"),
            abstentionsAllowed = parseBoolean(cells, "abstentionsAllowed"),
            duration = parseInt(cells, "duration"),
        )

    /** Blank lines are dropped, which also tolerates the line break a spreadsheet may append. */
    private fun parseList(cell: String?): List<String> =
        cell?.lines()?.filter { it.isNotBlank() } ?: listOf()

    private fun parseBoolean(cells: Map<String, String?>, column: String): Boolean? {
      val cell = cells[column]?.trim()
      if (cell.isNullOrEmpty()) return null
      return cell.lowercase().toBooleanStrictOrNull() ?: throw malformed(column)
    }

    private fun parseInt(cells: Map<String, String?>, column: String): Int? {
      val cell = cells[column]?.trim()
      if (cell.isNullOrEmpty()) return null
      return cell.toIntOrNull() ?: throw malformed(column)
    }

    private fun malformed(column: String) =
        ContentCsvRecordException(
            ContentCsvImportErrorReason.MALFORMED_RECORD, "Column $column has an invalid value")
  }
}
