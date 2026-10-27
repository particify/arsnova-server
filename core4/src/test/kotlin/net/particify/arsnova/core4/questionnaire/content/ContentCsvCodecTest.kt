/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonSubTypes
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation
import net.particify.arsnova.core4.questionnaire.content.csv.BinaryCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.ChoiceCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.FlashcardCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.NumericCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.PrioritizationCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.ScaleCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.ShortAnswerCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.SlideCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.SortCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.TextCsvCodec
import net.particify.arsnova.core4.questionnaire.content.csv.WordcloudCsvCodec
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val BODY = "Body"
private const val DURATION = 30
private const val SEED = 42

private val CODECS =
    listOf(
        BinaryCsvCodec(),
        ChoiceCsvCodec(),
        FlashcardCsvCodec(),
        NumericCsvCodec(),
        PrioritizationCsvCodec(),
        ScaleCsvCodec(),
        ShortAnswerCsvCodec(),
        SlideCsvCodec(),
        SortCsvCodec(),
        TextCsvCodec(),
        WordcloudCsvCodec(),
    )

class ContentCsvCodecTest {
  private val codecs = ContentCsvFormatCodecRegistry(CODECS)
  private val encoder = ContentCsvEncoder(codecs)
  private val decoder = ContentCsvDecoder(codecs)

  private val formatDataOfEachFormat =
      listOf(
          BinaryContentFormatData(true),
          BinaryContentFormatData(false),
          BinaryContentFormatData(null),
          ChoiceContentFormatData(listOf("A", "B", "C"), listOf(0, 2), true),
          ChoiceContentFormatData(listOf("A", "B"), listOf(), false),
          FlashcardContentFormatData("Back"),
          NumericContentFormatData(-5.0, 10.5, 2.25, 0.5),
          NumericContentFormatData(0.0, 100.0),
          PrioritizationContentFormatData(listOf("A", "B")),
          ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true),
          ScaleContentFormatData(ScaleContentFormatData.Template.PLUS_MINUS, false),
          ShortAnswerContentFormatData(listOf("Paris", "Lutetia")),
          SlideContentFormatData(),
          SortContentFormatData.shuffled(listOf("First", "Second", "Third"), Random(SEED)),
          TextContentFormatData(false),
          WordcloudContentFormatData(5),
      )

  private fun record(vararg cells: Pair<String, String?>) =
      ContentCsvRecord.fromCells(mapOf("body" to BODY, *cells))

  /** Goes through the cells as written, like a file exported and imported again. */
  private fun roundTrip(content: Content): DecodedContent {
    val cells = encoder.encode(content).toCells()
    return decoder.decode(ContentCsvRecord.fromCells(ContentCsvRecord.COLUMNS.zip(cells).toMap()))
  }

  private fun assertSameFormatData(expected: ContentFormatData, actual: ContentFormatData) {
    Assertions.assertEquals(expected.format, actual.format)
    when (expected) {
      is SortContentFormatData ->
          Assertions.assertEquals(
              expected.correctlyOrderedOptions(),
              (actual as SortContentFormatData).correctlyOrderedOptions())
      is WordcloudContentFormatData ->
          Assertions.assertEquals(
              expected.maxAnswers, (actual as WordcloudContentFormatData).maxAnswers)
      is SlideContentFormatData -> Unit
      else -> Assertions.assertEquals(expected, actual)
    }
  }

  private fun assertRejected(reason: ContentCsvImportErrorReason, decode: () -> Unit) {
    val exception = Assertions.assertThrows(ContentCsvRecordException::class.java, decode)
    Assertions.assertEquals(reason, exception.reason)
  }

  @Test
  fun shouldRoundTripEachFormat() {
    formatDataOfEachFormat.forEach { formatData ->
      val content =
          Content(
              body = BODY, formatData = formatData, duration = DURATION, abstentionsAllowed = false)
      val decoded = roundTrip(content)
      assertSameFormatData(formatData, decoded.formatData)
      Assertions.assertEquals(BODY, decoded.body)
      Assertions.assertFalse(decoded.abstentionsAllowed)
      Assertions.assertEquals(if (formatData.answerable()) DURATION else null, decoded.duration)
    }
  }

  @Test
  fun shouldRoundTripMultilineBodyAndMissingDuration() {
    val content =
        Content(body = "Line 1\nLine 2, \"quoted\"", formatData = TextContentFormatData(false))
    val decoded = roundTrip(content)
    Assertions.assertEquals(content.body, decoded.body)
    Assertions.assertNull(decoded.duration)
    Assertions.assertTrue(decoded.abstentionsAllowed)
  }

  @Test
  fun shouldExportAndImportFormulaLikeCellsVerbatim() {
    listOf("=1+1", "-1", "'quoted").forEach { body ->
      val content = Content(body = body, formatData = SlideContentFormatData())
      Assertions.assertEquals(body, encoder.encode(content).toCells()[1], body)
      Assertions.assertEquals(body, roundTrip(content).body, body)
    }
    val numeric = Content(body = BODY, formatData = NumericContentFormatData(-5.0, 10.5))
    Assertions.assertEquals("-5−10.5", encoder.encode(numeric).toCells()[3])
  }

  @Test
  fun shouldEncodeCellsLikeV3() {
    fun encode(formatData: ContentFormatData) =
        encoder.encode(Content(body = BODY, formatData = formatData))
    val numeric = encode(NumericContentFormatData(-5.0, 10.5, 2.25, 0.5))
    Assertions.assertEquals(listOf("-5−10.5"), numeric.options)
    Assertions.assertEquals(listOf("2.25±0.5"), numeric.correctOptions)
    val scale = encode(ScaleContentFormatData(ScaleContentFormatData.Template.EMOJI, false))
    Assertions.assertEquals(listOf("EMOJI (4)"), scale.options)
    val choice = encode(ChoiceContentFormatData(listOf("A", "B", "C"), listOf(2, 0), true))
    Assertions.assertEquals(listOf("A", "C"), choice.correctOptions)
    Assertions.assertEquals(true, choice.multiple)
    val binary = encode(BinaryContentFormatData(false))
    Assertions.assertEquals(BinaryCsvCodec.LABELS, binary.options)
    Assertions.assertEquals(listOf("No"), binary.correctOptions)
    Assertions.assertEquals("Back", encode(FlashcardContentFormatData("Back")).additionalText)
    Assertions.assertEquals(listOf("3"), encode(WordcloudContentFormatData(3)).options)
    val sort = encode(SortContentFormatData(listOf("B", "C", "A"), listOf(2, 0, 1)))
    Assertions.assertEquals(listOf("A", "B", "C"), sort.options)
    Assertions.assertEquals(listOf("A", "B", "C"), sort.correctOptions)
    Assertions.assertEquals(0, encode(TextContentFormatData(false)).duration)
  }

  @Test
  fun shouldAcceptV3NumericVariants() {
    fun decode(range: String, correct: String?) =
        decoder
            .decode(record("format" to "NUMERIC", "options" to range, "correctOptions" to correct))
            .formatData
    Assertions.assertEquals(NumericContentFormatData(1.0, 10.0, 5.0, 1.0), decode("1−10", "5±1"))
    Assertions.assertEquals(NumericContentFormatData(1.0, 10.0, 5.0, 1.0), decode("1-10", "5+-1"))
    Assertions.assertEquals(NumericContentFormatData(1.0, 10.0, 5.0, 1.0), decode("1–10", "5+−1"))
    Assertions.assertEquals(NumericContentFormatData(-10.0, -1.0, -5.0), decode("-10−-1", "-5"))
    Assertions.assertEquals(NumericContentFormatData(0.5, 2.5), decode("0,5 - 2,5", null))
  }

  @Test
  fun shouldDecodeV3ListCells() {
    val choice =
        decoder.decode(
            record(
                "format" to "choice",
                "options" to "A\r\nB\nC\n",
                "correctOptions" to "C\nA",
                "multiple" to "TRUE"))
    Assertions.assertEquals(
        ChoiceContentFormatData(listOf("A", "B", "C"), listOf(0, 2), true), choice.formatData)
    val shortAnswer =
        decoder.decode(record("format" to "SHORT_ANSWER", "correctOptions" to "Paris"))
    Assertions.assertEquals(ShortAnswerContentFormatData(listOf("Paris")), shortAnswer.formatData)
    val binary =
        decoder.decode(
            record("format" to "BINARY", "options" to "Ja\nNein", "correctOptions" to "Nein"))
    Assertions.assertEquals(BinaryContentFormatData(false), binary.formatData)
  }

  @Test
  fun shouldReadScaleWithoutTemplateAsChoiceLikeV3() {
    val decoded = decoder.decode(record("format" to "SCALE", "options" to "Agree\nDisagree"))
    Assertions.assertEquals(
        ChoiceContentFormatData(listOf("Agree", "Disagree"), listOf(), false), decoded.formatData)
  }

  @Test
  fun shouldStoreNonAnswerableFormatsWithoutDurationOrAbstentions() {
    val decoded =
        decoder.decode(
            record(
                "format" to "FLASHCARD",
                "additionalText" to "Back",
                "duration" to "60",
                "abstentionsAllowed" to "true"))
    Assertions.assertNull(decoded.duration)
    Assertions.assertFalse(decoded.abstentionsAllowed)
  }

  @Test
  fun shouldReadZeroDurationAsNone() {
    val decoded = decoder.decode(record("format" to "TEXT", "duration" to "0"))
    Assertions.assertNull(decoded.duration)
  }

  @Test
  fun shouldMatchColumnNamesRegardlessOfCase() {
    Assertions.assertEquals("format", ContentCsvRecord.columnOf("Format"))
    Assertions.assertEquals("correctOptions", ContentCsvRecord.columnOf(" CORRECTOPTIONS "))
    Assertions.assertEquals("abstentionsAllowed", ContentCsvRecord.columnOf("abstentionsallowed"))
    Assertions.assertNull(ContentCsvRecord.columnOf("comment"))
    Assertions.assertNull(ContentCsvRecord.columnOf(""))
    Assertions.assertNull(ContentCsvRecord.columnOf(null))
  }

  @Test
  fun shouldRejectUnknownFormatsAndMissingBody() {
    assertRejected(ContentCsvImportErrorReason.UNKNOWN_FORMAT) {
      decoder.decode(record("format" to "GRID"))
    }
    assertRejected(ContentCsvImportErrorReason.UNKNOWN_FORMAT) { decoder.decode(record()) }
    assertRejected(ContentCsvImportErrorReason.MISSING_BODY) {
      decoder.decode(ContentCsvRecord.fromCells(mapOf("format" to "TEXT", "body" to " ")))
    }
  }

  @Test
  fun shouldRejectMalformedCells() {
    assertRejected(ContentCsvImportErrorReason.MALFORMED_RECORD) {
      record("format" to "CHOICE", "multiple" to "maybe")
    }
    assertRejected(ContentCsvImportErrorReason.MALFORMED_RECORD) {
      record("format" to "TEXT", "duration" to "1 minute")
    }
  }

  @Test
  fun shouldRejectInvalidFormatData() {
    listOf(
            record("format" to "BINARY", "options" to "Yes\nNo\nMaybe"),
            record("format" to "CHOICE", "options" to "A"),
            record("format" to "CHOICE", "options" to "A\nB", "correctOptions" to "C"),
            record("format" to "FLASHCARD"),
            record("format" to "NUMERIC", "options" to "ten to twenty"),
            record("format" to "NUMERIC", "options" to "10−1"),
            record("format" to "PRIORITIZATION", "options" to "A\nA"),
            record("format" to "SCALE", "options" to "UNKNOWN (5)"),
            record("format" to "SHORT_ANSWER", "options" to "A", "correctOptions" to "B"),
            record("format" to "SORT", "options" to "A"),
            record("format" to "TEXT", "duration" to "${Content.MAX_DURATION + 1}"),
            record("format" to "WORDCLOUD", "options" to "many"),
        )
        .forEach {
          assertRejected(ContentCsvImportErrorReason.INVALID_FORMAT_DATA) { decoder.decode(it) }
        }
  }

  @Test
  fun shouldHaveACodecForEachFormat() {
    val subTypes = ContentFormatData::class.findAnnotation<JsonSubTypes>()!!.value
    Assertions.assertEquals(
        subTypes.associate { it.name to it.value }, CODECS.associate { it.format to it.type })
  }

  @Test
  fun shouldRejectCodecsOfTheSameFormat() {
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      ContentCsvFormatCodecRegistry(listOf(TextCsvCodec(), TextCsvCodec()))
    }
  }

  @Test
  fun shouldFailToExportAFormatWithoutCodec() {
    val textOnly = ContentCsvEncoder(ContentCsvFormatCodecRegistry(listOf(TextCsvCodec())))
    Assertions.assertThrows(IllegalStateException::class.java) {
      textOnly.encode(Content(body = BODY, formatData = SlideContentFormatData()))
    }
  }
}
