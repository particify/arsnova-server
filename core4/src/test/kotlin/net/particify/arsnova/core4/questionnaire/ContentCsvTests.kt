/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.DelimitedTextFormat
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentCsvImportErrorReason
import net.particify.arsnova.core4.questionnaire.content.ContentCsvImportResult
import net.particify.arsnova.core4.questionnaire.content.ContentCsvService
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.security.internal.JwtUtils
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Accounts and rooms from the `dev` Liquibase fixtures. */
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val PARTICIPANT_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Owned by [OWNER_ID]; [PARTICIPANT_ID] is a participant. */
private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")

private const val HEADER =
    "format,body,additionalText,options,correctOptions,multiple,abstentionsAllowed,duration"

/** A file as exported by v3, which separates numeric ranges with the minus sign U+2212. */
private val V3_CSV =
    """
    $HEADER
    CHOICE,Which are primes?,,"2
    3
    4","2
    3",true,true,60
    BINARY,Is it sunny?,,"Yes
    No",Yes,false,true,0
    SCALE,How do you feel?,,AGREEMENT (5),,false,true,0
    NUMERIC,Guess the number,,1−10,5±1,false,false,0
    WORDCLOUD,Your words,,3,,false,true,0
    SHORT_ANSWER,Capital of France?,,"Paris
    Lutetia","Paris
    Lutetia",false,true,0
    SORT,Order these,,"First
    Second
    Third","First
    Second
    Third",false,true,0
    PRIORITIZATION,Prioritize,,"A
    B",,false,true,0
    TEXT,Tell us,,,,false,true,0
    SLIDE,Welcome,,,,false,false,0
    FLASHCARD,Front,Back,,,false,false,0
    """
        .trimIndent()

private val V3_TSV =
    HEADER.replace(',', '\t') +
        "\nCHOICE\tWhich are primes?\t\t\"2\n3\n4\"\t\"2\n3\"\ttrue\ttrue\t60" +
        "\nSCALE\tHow do you feel?\t\tAGREEMENT (5)\t\tfalse\ttrue\t0" +
        "\nFLASHCARD\tFront\tBack\t\t\tfalse\tfalse\t0\n"

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ContentCsvTests {
  @Autowired lateinit var contentCsvService: ContentCsvService
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateOwner() {
    authenticate(OWNER_ID)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID) {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  private fun createQuestionnaire(type: QuestionnaireType = QuestionnaireType.MIXED) =
      questionnaireService.create(ROOM_ID, "CSV ${UUID.randomUUID()}", type)

  private fun import(questionnaire: Questionnaire, text: String) =
      contentCsvService.import(questionnaire.id!!, text)

  private fun contentsOf(questionnaire: Questionnaire): List<Content> =
      contentService.findAllByQuestionnaireIdOrderByPosition(questionnaire.id!!)

  private fun formatDataOf(questionnaire: Questionnaire) =
      contentsOf(questionnaire).map { it.formatData!! }

  private fun assertErrors(
      expected: List<Pair<Int, ContentCsvImportErrorReason>>,
      result: ContentCsvImportResult
  ) {
    Assertions.assertEquals(expected, result.errorLines.map { it.line to it.reason }) {
      result.errorLines.toString()
    }
  }

  @Test
  fun shouldImportV3CsvExport() {
    val questionnaire = createQuestionnaire()
    val result = import(questionnaire, V3_CSV)
    Assertions.assertEquals(ContentCsvImportResult(11, 11, listOf()), result)
    val contents = contentsOf(questionnaire)
    Assertions.assertEquals(
        listOf(
            "CHOICE",
            "BINARY",
            "SCALE",
            "NUMERIC",
            "WORDCLOUD",
            "SHORT_ANSWER",
            "SORT",
            "PRIORITIZATION",
            "TEXT",
            "SLIDE",
            "FLASHCARD"),
        contents.map { it.formatData!!.format })
    Assertions.assertEquals(
        ChoiceContentFormatData(listOf("2", "3", "4"), listOf(0, 1), true), contents[0].formatData)
    Assertions.assertEquals(60, contents[0].duration)
    Assertions.assertEquals(BinaryContentFormatData(true), contents[1].formatData)
    Assertions.assertEquals(
        ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true),
        contents[2].formatData)
    Assertions.assertEquals(NumericContentFormatData(1.0, 10.0, 5.0, 1.0), contents[3].formatData)
    Assertions.assertFalse(contents[3].abstentionsAllowed)
    Assertions.assertEquals(3, (contents[4].formatData as WordcloudContentFormatData).maxAnswers)
    Assertions.assertEquals(
        ShortAnswerContentFormatData(listOf("Paris", "Lutetia")), contents[5].formatData)
    Assertions.assertEquals(
        listOf("First", "Second", "Third"),
        (contents[6].formatData as SortContentFormatData).correctlyOrderedOptions())
    Assertions.assertEquals(
        PrioritizationContentFormatData(listOf("A", "B")), contents[7].formatData)
    Assertions.assertEquals(TextContentFormatData(false), contents[8].formatData)
    Assertions.assertEquals(FlashcardContentFormatData("Back"), contents[10].formatData)
    Assertions.assertTrue(contents.all { it.duration == null || it === contents[0] })
  }

  @Test
  fun shouldImportV3TsvExport() {
    val questionnaire = createQuestionnaire()
    val result = import(questionnaire, V3_TSV)
    Assertions.assertEquals(ContentCsvImportResult(3, 3, listOf()), result)
    Assertions.assertEquals(
        listOf(
            ChoiceContentFormatData(listOf("2", "3", "4"), listOf(0, 1), true),
            ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true),
            FlashcardContentFormatData("Back")),
        formatDataOf(questionnaire))
  }

  @Test
  fun shouldDetectSeparatorFromHeader() {
    val semicolons = createQuestionnaire()
    val text = "\uFEFFformat;body;options\nCHOICE;\"Pick; one\";\"A\nB\"\nTEXT;Why?;"
    Assertions.assertEquals(ContentCsvImportResult(2, 2, listOf()), import(semicolons, text))
    Assertions.assertEquals("Pick; one", contentsOf(semicolons)[0].body)
    val tabs = createQuestionnaire()
    val tsvAsCsv = import(tabs, "format\tbody\nTEXT\tWhy, though?")
    Assertions.assertEquals(1, tsvAsCsv.importedLines)
    Assertions.assertEquals("Why, though?", contentsOf(tabs)[0].body)
  }

  @Test
  fun shouldReportEachInvalidRecordAndImportTheOthers() {
    val questionnaire = createQuestionnaire()
    val text =
        listOf(
                "format,body,options,multiple",
                "TEXT,First",
                "GRID,Grid",
                "",
                "TEXT,\" \"",
                "CHOICE,Too few,A",
                "CHOICE,Unsure,\"A\nB\",maybe",
                "TEXT,\"Broken\"quote",
                "TEXT,Too,many,cells,here",
                "SLIDE,Last")
            .joinToString("\n")
    val result = import(questionnaire, text)
    Assertions.assertEquals(8, result.totalLines)
    Assertions.assertEquals(2, result.importedLines)
    assertErrors(
        listOf(
            2 to ContentCsvImportErrorReason.UNKNOWN_FORMAT,
            3 to ContentCsvImportErrorReason.MISSING_BODY,
            4 to ContentCsvImportErrorReason.INVALID_FORMAT_DATA,
            5 to ContentCsvImportErrorReason.MALFORMED_RECORD,
            6 to ContentCsvImportErrorReason.MALFORMED_RECORD,
            7 to ContentCsvImportErrorReason.MALFORMED_RECORD),
        result)
    Assertions.assertEquals(listOf("First", "Last"), contentsOf(questionnaire).map { it.body })
  }

  @Test
  fun shouldSkipFormatsTheQuestionnaireTypeDoesNotAccept() {
    val quiz = createQuestionnaire(QuestionnaireType.QUIZ)
    val text =
        listOf(
                "format,body,options,correctOptions",
                "TEXT,Survey only,,",
                "CHOICE,Without solution,\"A\nB\",",
                "CHOICE,With solution,\"A\nB\",B",
                "SLIDE,Anywhere,,")
            .joinToString("\n")
    val result = import(quiz, text)
    Assertions.assertEquals(2, result.importedLines)
    assertErrors(
        listOf(
            1 to ContentCsvImportErrorReason.INCOMPATIBLE_FORMAT,
            2 to ContentCsvImportErrorReason.INCOMPATIBLE_FORMAT),
        result)
    Assertions.assertEquals(listOf("With solution", "Anywhere"), contentsOf(quiz).map { it.body })
  }

  @Test
  fun shouldExportInPositionOrderWithV3Header() {
    val questionnaire = createQuestionnaire()
    import(questionnaire, V3_CSV)
    val exported = contentCsvService.export(questionnaire.id!!, null, DelimitedTextFormat.CSV)
    Assertions.assertEquals(HEADER, exported.lineSequence().first())
    val copy = createQuestionnaire()
    Assertions.assertEquals(ContentCsvImportResult(11, 11, listOf()), import(copy, exported))
    Assertions.assertEquals(
        contentsOf(questionnaire).map { it.body }, contentsOf(copy).map { it.body })
  }

  @Test
  fun shouldExportOnlySelectedContentsOfTheQuestionnaire() {
    val questionnaire = createQuestionnaire()
    import(questionnaire, V3_TSV)
    val other = createQuestionnaire()
    import(other, V3_TSV)
    val selected = contentsOf(questionnaire)[1]
    val exported =
        contentCsvService.export(
            questionnaire.id!!,
            listOf(contentsOf(other)[0].id!!, selected.id!!),
            DelimitedTextFormat.TSV)
    val copy = createQuestionnaire()
    Assertions.assertEquals(ContentCsvImportResult(1, 1, listOf()), import(copy, exported))
    Assertions.assertEquals(listOf(selected.formatData), formatDataOf(copy))
  }

  private fun tooManyRecords() =
      (listOf("format,body") + (0..ContentCsvService.MAX_RECORDS).map { "SLIDE,Slide $it" })
          .joinToString("\n")

  @Test
  fun shouldRejectMoreRecordsThanTheCapBeforeCreatingAny() {
    val questionnaire = createQuestionnaire()
    Assertions.assertThrows(InvalidInputException::class.java) {
      import(questionnaire, tooManyRecords())
    }
    Assertions.assertEquals(listOf<Content>(), contentsOf(questionnaire))
  }

  /**
   * The test's own authentication is put aside while a request runs: MockMvc would hand it to the
   * request, whose JWT filter then keeps it rather than reading the token. It is restored
   * afterwards since the filter chain clears the context.
   */
  private fun perform(request: RequestBuilder): ResultActions {
    val context = SecurityContextHolder.getContext()
    TestSecurityContextHolder.clearContext()
    try {
      return mockMvc.perform(request)
    } finally {
      TestSecurityContextHolder.setContext(context)
    }
  }

  private fun <B : AbstractMockHttpServletRequestBuilder<B>> asUser(userId: UUID, request: B): B =
      request.header(
          HttpHeaders.AUTHORIZATION, "Bearer ${jwtUtils.encodeJwt(userId.toString(), listOf())}")

  private fun exportRequest(questionnaireId: UUID, vararg params: Pair<String, String>) =
      asUser(
          OWNER_ID,
          get("/questionnaire/$questionnaireId/contents-export").apply {
            params.forEach { (name, value) -> param(name, value) }
          })

  private fun importRequest(questionnaireId: UUID, bytes: ByteArray, userId: UUID = OWNER_ID) =
      asUser(
          userId,
          multipart("/questionnaire/$questionnaireId/contents-import")
              .file(MockMultipartFile("file", "contents.csv", "text/csv", bytes)))

  @Test
  fun shouldExportAndImportOverHttp() {
    val source = createQuestionnaire()
    contentService.create(source.id!!, "Slide", SlideContentFormatData(), null, false, null)
    val exported =
        perform(exportRequest(source.id!!, "format" to "TSV", "charset" to "UTF-16LE"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(HttpHeaders.CONTENT_TYPE, "text/tab-separated-values;charset=UTF-16LE"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contents.tsv\""))
            .andReturn()
            .response
            .contentAsByteArray
    Assertions.assertEquals(listOf(0xFF.toByte(), 0xFE.toByte()), exported.take(2))
    val target = createQuestionnaire()
    perform(importRequest(target.id!!, exported))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLines").value(1))
        .andExpect(jsonPath("$.importedLines").value(1))
        .andExpect(jsonPath("$.errorLines").isEmpty())
    Assertions.assertEquals(listOf("Slide"), contentsOf(target).map { it.body })
  }

  @Test
  fun shouldExportCsvInUtf8WithoutByteOrderMarkByDefault() {
    val questionnaire = createQuestionnaire()
    contentService.create(questionnaire.id!!, "=1+1", SlideContentFormatData(), null, false, null)
    val exported =
        perform(exportRequest(questionnaire.id!!))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
            .andReturn()
            .response
            .contentAsByteArray
            .toString(Charsets.UTF_8)
    Assertions.assertEquals(HEADER, exported.lineSequence().first())
    // Quoted for the "+", which a spreadsheet strips before evaluating the formula.
    Assertions.assertTrue(exported.lines()[1].startsWith("SLIDE,\"=1+1\","), exported)
  }

  @Test
  fun shouldReportImportErrorsAsJson() {
    val questionnaire = createQuestionnaire()
    perform(importRequest(questionnaire.id!!, "format,body\nGRID,Grid".toByteArray()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errorLines[0].line").value(1))
        .andExpect(jsonPath("$.errorLines[0].reason").value("UNKNOWN_FORMAT"))
  }

  @Test
  fun shouldRejectInvalidRequestsWithTheirStatus() {
    val questionnaire = createQuestionnaire()
    perform(exportRequest(questionnaire.id!!, "format" to "xlsx"))
        .andExpect(status().isBadRequest())
    perform(exportRequest(questionnaire.id!!, "charset" to "ISO-8859-1"))
        .andExpect(status().isBadRequest())
    perform(exportRequest(UUID.randomUUID())).andExpect(status().isNotFound())
    perform(asUser(OWNER_ID, multipart("/questionnaire/${questionnaire.id}/contents-import")))
        .andExpect(status().isBadRequest())
    perform(importRequest(questionnaire.id!!, tooManyRecords().toByteArray()))
        .andExpect(status().isBadRequest())
    Assertions.assertEquals(listOf<Content>(), contentsOf(questionnaire))
  }

  @Test
  fun shouldDenyParticipant() {
    val questionnaire = createQuestionnaire()
    perform(asUser(PARTICIPANT_ID, get("/questionnaire/${questionnaire.id}/contents-export")))
        .andExpect(status().isForbidden())
    perform(importRequest(questionnaire.id!!, V3_CSV.toByteArray(), PARTICIPANT_ID))
        .andExpect(status().isForbidden())
    Assertions.assertEquals(listOf<Content>(), contentsOf(questionnaire))
  }
}
