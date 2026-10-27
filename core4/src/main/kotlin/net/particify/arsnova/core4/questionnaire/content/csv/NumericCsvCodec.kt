/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.csv

import java.math.BigDecimal
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCells
import net.particify.arsnova.core4.questionnaire.content.ContentCsvFormatCodec
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import org.springframework.stereotype.Component

/**
 * The range is written as `min−max` and the correct number as `correct±tolerance`, separated by the
 * minus sign U+2212 and the plus-minus sign U+00B1 like the v3 export does. Reading also accepts
 * the hyphen, the en dash and `+-` as separators, a decimal comma and a missing tolerance.
 */
@Component
class NumericCsvCodec : ContentCsvFormatCodec<NumericContentFormatData> {
  override val format = "NUMERIC"
  override val type = NumericContentFormatData::class

  override fun encode(formatData: NumericContentFormatData) =
      ContentCsvFormatCells(
          options = listOf(encodeRange(formatData)),
          correctOptions = listOfNotNull(encodeCorrectNumber(formatData)))

  override fun decode(cells: ContentCsvFormatCells): NumericContentFormatData {
    val range =
        cells.options.singleOrNull()?.let { RANGE.matchEntire(it) }
            ?: throw invalid("A numeric content needs a single range like 1−10")
    if (cells.correctOptions.size > 1) {
      throw invalid("A numeric content has at most one correct number")
    }
    val correct =
        cells.correctOptions.singleOrNull()?.let {
          CORRECT_NUMBER.matchEntire(it)
              ?: throw invalid("The correct number must be given like 5±1")
        }
    return NumericContentFormatData(
        minNumber = parse(range.groupValues[1]),
        maxNumber = parse(range.groupValues[2]),
        correctNumber = correct?.let { parse(it.groupValues[1]) },
        tolerance = correct?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }?.let(::parse) ?: 0.0,
    )
  }

  private fun encodeRange(formatData: NumericContentFormatData): String =
      "${format(formatData.minNumber)}−${format(formatData.maxNumber)}"

  private fun encodeCorrectNumber(formatData: NumericContentFormatData): String? =
      formatData.correctNumber?.let { "${format(it)}±${format(formatData.tolerance)}" }

  /** Unlike v3, which rounded to one decimal place, every digit is kept. */
  private fun format(number: Double): String =
      BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()

  private fun parse(number: String): Double = number.replace(',', '.').toDouble()

  companion object {
    private const val NUMBER = """[-+]?(?:\d+(?:[.,]\d*)?|[.,]\d+)"""
    private val RANGE = Regex("""\s*($NUMBER)\s*[−–-]\s*($NUMBER)\s*""")
    private val CORRECT_NUMBER = Regex("""\s*($NUMBER)\s*(?:(?:±|\+[−–-])\s*($NUMBER)\s*)?""")
  }
}
