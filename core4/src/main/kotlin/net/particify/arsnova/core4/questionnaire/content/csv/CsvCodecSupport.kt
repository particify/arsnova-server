/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvImportErrorReason
import net.particify.arsnova.core4.questionnaire.content.ContentCsvRecordException

private const val MIN_CHOICE_OPTION_COUNT = 2

internal fun decodeChoice(cells: ContentCsvFormatCells): ChoiceContentFormatData {
  if (cells.options.size < MIN_CHOICE_OPTION_COUNT) {
    throw invalid("A choice content must have at least $MIN_CHOICE_OPTION_COUNT options")
  }
  return ChoiceContentFormatData(cells.options, correctIndexes(cells), cells.multiple)
}

internal fun correctIndexes(cells: ContentCsvFormatCells): List<Int> =
    cells.correctOptions
        .map { label ->
          cells.options.indexOf(label).takeIf { it >= 0 }
              ?: throw invalid("Correct options must be among the options")
        }
        .distinct()
        .sorted()

internal fun invalid(message: String) =
    ContentCsvRecordException(ContentCsvImportErrorReason.INVALID_FORMAT_DATA, message)
