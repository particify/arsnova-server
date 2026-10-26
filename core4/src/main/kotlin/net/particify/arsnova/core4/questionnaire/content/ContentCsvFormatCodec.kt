/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import kotlin.reflect.KClass

/**
 * Writes the format data of one format to the format cells of a [ContentCsvRecord] and reads it
 * back. A format without a codec can be neither exported nor imported.
 */
interface ContentCsvFormatCodec<T : ContentFormatData> {
  /** The format's name, which is both its [ContentFormatData.format] and its format cell. */
  val format: String
  val type: KClass<T>

  fun encode(formatData: T): ContentCsvFormatCells

  /**
   * May return format data of another format, as v3 files require for scale contents. Throws
   * [ContentCsvRecordException] for cells which describe no format data.
   */
  fun decode(cells: ContentCsvFormatCells): ContentFormatData
}

/** The cells of a [ContentCsvRecord] whose meaning depends on the format. */
data class ContentCsvFormatCells(
    val additionalText: String? = null,
    val options: List<String> = listOf(),
    val correctOptions: List<String> = listOf(),
    val multiple: Boolean = false,
)
