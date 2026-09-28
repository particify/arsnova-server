/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30
private const val BODY = "Order these cities from north to south."
private const val OTHER_BODY = "Order these cities from south to north."
private val OPTIONS = listOf("Hamburg", "Frankfurt am Main", "Munich")
private val OTHER_OPTIONS = listOf("Bonn", "Cologne", "Offenbach")
private val CORRECT_ORDER = listOf(0, 1, 2)
private val PERMUTED_ORDER = listOf(1, 0, 2)

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class SortContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    authenticate(OWNER_ID)
  }

  private fun authenticate(userId: UUID) {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Sort ${UUID.randomUUID()}", type)

  private fun createSortContent(options: List<String> = OPTIONS): Content =
      contentService.create(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!,
          BODY,
          SortContentFormatData(options),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answer(content: Content, selectedChoiceIndexes: List<Int>): Answer {
    authenticate(userService.createAccount().id!!)
    try {
      return answerService.createAnswer(
          content.activeRound!!.id!!, SortAnswerFormatData(selectedChoiceIndexes))
    } finally {
      authenticate(OWNER_ID)
    }
  }

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) = reload(content).formatData as SortContentFormatData

  @Test
  fun shouldPersistAndReloadSortFormat() {
    val content = createSortContent()
    val formatData = formatDataOf(content)
    Assertions.assertEquals("SORT", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertEquals(OPTIONS, formatData.options)
    Assertions.assertEquals(Content.BASE_POINTS, reload(content).points)
  }

  @Test
  fun shouldPersistAndReloadSortAnswerFormat() {
    val answer = answer(createSortContent(), PERMUTED_ORDER)
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("SORT", reloaded.formatData!!.format)
    Assertions.assertEquals(
        PERMUTED_ORDER, (reloaded.formatData as SortAnswerFormatData).selectedChoiceIndexes)
  }

  @Test
  fun shouldScoreDeclaredOptionOrderAsCorrect() {
    val answer = answer(createSortContent(), CORRECT_ORDER)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
  }

  @Test
  fun shouldScoreOtherOrderAsWrongWithoutPoints() {
    val answer = answer(createSortContent(), PERMUTED_ORDER)
    Assertions.assertEquals(AnswerResultState.WRONG, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldCreateAbstention() {
    val content = createSortContent()
    val answer = answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldRejectContentWithTooFewOptions() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createSortContent(listOf("Hamburg"))
    }
  }

  @Test
  fun shouldRejectBlankOption() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createSortContent(listOf("Hamburg", " "))
    }
  }

  @Test
  fun shouldRejectRepeatedOption() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createSortContent(listOf("Hamburg", "Hamburg"))
    }
  }

  @Test
  fun shouldRejectAnswerRepeatingAnIndex() {
    val content = createSortContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(0, 1, 1))
    }
  }

  @Test
  fun shouldRejectAnswerOmittingAnIndex() {
    val content = createSortContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(0, 1))
    }
  }

  @Test
  fun shouldRejectAnswerWithIndexOutOfRange() {
    val content = createSortContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(0, 1, 3))
    }
  }

  @Test
  fun shouldUpdateOptionsWithoutAnswers() {
    val content = createSortContent()
    contentService.update(
        content.id!!, BODY, SortContentFormatData(OTHER_OPTIONS), null, true, null)
    Assertions.assertEquals(OTHER_OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectOptionChangeOnAnsweredContent() {
    val content = createSortContent()
    answer(content, CORRECT_ORDER)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, SortContentFormatData(OTHER_OPTIONS), null, true, null)
    }
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectOptionReorderingOnAnsweredContent() {
    val content = createSortContent()
    answer(content, CORRECT_ORDER)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, SortContentFormatData(OPTIONS.reversed()), null, true, null)
    }
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldUpdateUnrelatedFieldsOnAnsweredContent() {
    val content = createSortContent()
    answer(content, CORRECT_ORDER)
    contentService.update(
        content.id!!, OTHER_BODY, SortContentFormatData(OPTIONS), DURATION, false, null)
    val reloaded = reload(content)
    Assertions.assertEquals(OTHER_BODY, reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectSortUpdateForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(content.id!!, BODY, SortContentFormatData(OPTIONS), null, true, null)
    }
  }

  @Test
  fun shouldRejectSortAnswerForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, CORRECT_ORDER)
    }
  }

  private fun createTextContent(): Content =
      contentService.create(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!,
          BODY,
          TextContentFormatData(false),
          null,
          true,
          null,
      )
}
