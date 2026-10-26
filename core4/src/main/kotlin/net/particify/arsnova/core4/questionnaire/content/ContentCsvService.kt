/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import net.particify.arsnova.core4.common.DelimitedTextFormat
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.databind.MappingIterator
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvReadFeature
import tools.jackson.dataformat.csv.CsvSchema

/** Exports and imports the contents of a questionnaire as delimiter-separated values. */
@Service
class ContentCsvService(
    private val contentCsvDecoder: ContentCsvDecoder,
    private val contentCsvEncoder: ContentCsvEncoder,
    private val contentService: ContentServiceImpl,
    private val questionnaireRepository: QuestionnaireRepository,
) {
  private val mapper = CsvMapper.builder().enable(CsvReadFeature.WRAP_AS_ARRAY).build()

  /**
   * Exports all contents, or those of [contentIds] which belong to the questionnaire.
   *
   * Cells are written unescaped, so a spreadsheet application may evaluate one starting with `=`,
   * `+`, `-` or `@` as a formula (CSV injection). Guarding against that is left to the spreadsheet
   * application.
   */
  fun export(
      questionnaireId: UUID,
      contentIds: Collection<UUID>?,
      format: DelimitedTextFormat
  ): String {
    val selectedIds = contentIds?.toSet()
    val records =
        contentService
            .findAllByQuestionnaireIdOrderByPosition(questionnaireId)
            .filter { selectedIds == null || it.id in selectedIds }
            .map { contentCsvEncoder.encode(it).toCells() }
    val schema =
        CsvSchema.builder()
            .apply { ContentCsvRecord.COLUMNS.forEach { addColumn(it) } }
            .build()
            .withHeader()
            .withColumnSeparator(format.separator)
    return mapper.writer(schema).writeValueAsString(records)
  }

  /**
   * Checks every record before creating the contents of the valid ones, so that an invalid record
   * does not abort the import. Not transactional on purpose: each content is created in a
   * transaction of its own, as a failure inside a shared one would roll back all of them.
   *
   * A file of more than [MAX_RECORDS] records is rejected as a whole before anything is created.
   */
  fun import(questionnaireId: UUID, text: String): ContentCsvImportResult {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val records = readRecords(text)
    if (records.size > MAX_RECORDS) {
      throw InvalidInputException("A file may hold at most $MAX_RECORDS contents")
    }
    val lines = records.map { decode(it, questionnaire) }
    val errors = lines.mapNotNull { it.error }.toMutableList()
    var importedLines = 0
    lines
        .filter { it.content != null }
        .forEach { line ->
          val error = create(questionnaireId, line.number, line.content!!)
          if (error == null) importedLines++ else errors.add(error)
        }
    return ContentCsvImportResult(lines.size, importedLines, errors.sortedBy { it.line })
  }

  private fun create(questionnaireId: UUID, number: Int, content: DecodedContent) =
      try {
        contentService.create(
            questionnaireId,
            content.body,
            content.formatData,
            content.duration,
            content.abstentionsAllowed,
            null)
        null
      } catch (e: ContentFormatDataInvalidException) {
        ContentCsvImportError(number, ContentCsvImportErrorReason.INVALID_FORMAT_DATA, e.message)
      }

  private fun decode(record: NumberedRecord, questionnaire: Questionnaire): ImportLine =
      try {
        val content = contentCsvDecoder.decode(record.parse())
        if (questionnaire.type?.accepts(content.formatData) == false) {
          throw ContentCsvRecordException(
              ContentCsvImportErrorReason.INCOMPATIBLE_FORMAT,
              "A ${questionnaire.type} questionnaire does not accept this " +
                  "${content.formatData.format} content")
        }
        ImportLine(record.number, content, null)
      } catch (e: ContentCsvRecordException) {
        ImportLine(record.number, null, ContentCsvImportError(record.number, e.reason, e.message))
      }

  /** The first row is the header. Blank rows are no records and are skipped. */
  private fun readRecords(text: String): List<NumberedRecord> {
    val content = text.removePrefix(BYTE_ORDER_MARK)
    val schema = CsvSchema.emptySchema().withColumnSeparator(detectSeparator(content))
    val rows =
        readRows(mapper.readerFor(List::class.java).with(schema).readValues(content)).filterNot {
          it is CsvRow.Values && it.cells.all(String?::isNullOrBlank)
        }
    val header = (rows.firstOrNull() as? CsvRow.Values)?.cells?.map { it?.trim() }.orEmpty()
    return rows.drop(1).mapIndexed { index, row -> NumberedRecord(index + 1, header, row) }
  }

  /**
   * Reading resumes after a malformed row. It stops once a row fails at the same position as the
   * one before, in case the parser cannot make progress past it.
   */
  private fun readRows(iterator: MappingIterator<List<Any?>>): List<CsvRow> {
    val rows = mutableListOf<CsvRow>()
    var row = readRow(iterator)
    while (row != null && !(row is CsvRow.Malformed && row == rows.lastOrNull())) {
      rows.add(row)
      row = readRow(iterator)
    }
    return rows
  }

  private fun readRow(iterator: MappingIterator<List<Any?>>): CsvRow? =
      try {
        if (iterator.hasNextValue()) CsvRow.Values(iterator.nextValue().map { it?.toString() })
        else null
      } catch (e: JacksonException) {
        CsvRow.Malformed(e.originalMessage, iterator.currentLocation().charOffset)
      }

  /**
   * Like v3, the separator is detected from the header, which holds none of the candidates in its
   * column names. A tab takes precedence, then a semicolon as spreadsheets in many locales use it
   * in place of a comma. A header of a single column has none, and then the comma is as good as
   * any.
   */
  private fun detectSeparator(text: String): Char {
    val header = text.lineSequence().firstOrNull().orEmpty()
    return SEPARATORS.firstOrNull { it in header } ?: DelimitedTextFormat.CSV.separator
  }

  private sealed interface CsvRow {
    data class Values(val cells: List<String?>) : CsvRow

    data class Malformed(val message: String?, val offset: Long) : CsvRow
  }

  private class NumberedRecord(val number: Int, val header: List<String?>, val row: CsvRow) {
    fun parse(): ContentCsvRecord =
        when (row) {
          is CsvRow.Malformed ->
              throw ContentCsvRecordException(
                  ContentCsvImportErrorReason.MALFORMED_RECORD, row.message ?: "Malformed record")
          is CsvRow.Values -> {
            if (row.cells.size > header.size) {
              throw ContentCsvRecordException(
                  ContentCsvImportErrorReason.MALFORMED_RECORD,
                  "Record has ${row.cells.size} cells but the header names ${header.size} columns")
            }
            ContentCsvRecord.fromCells(
                row.cells.withIndex().associate { (index, cell) ->
                  header[index].orEmpty() to cell
                })
          }
        }
  }

  private class ImportLine(
      val number: Int,
      val content: DecodedContent?,
      val error: ContentCsvImportError?
  )

  companion object {
    const val MAX_RECORDS = 500
    private const val BYTE_ORDER_MARK = "﻿"
    private val SEPARATORS = listOf('\t', ';', ',')
  }
}
