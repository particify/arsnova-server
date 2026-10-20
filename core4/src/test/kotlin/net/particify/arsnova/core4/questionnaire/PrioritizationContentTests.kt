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
import net.particify.arsnova.core4.questionnaire.answer.PrioritizationAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30
private const val BODY = "How should the budget be split?"
private const val OTHER_BODY = "How would you split the budget?"
private val OPTIONS = listOf("Bicycle lanes", "Public transport", "Car parking")
private val OTHER_OPTIONS = listOf("Playgrounds", "Libraries", "Swimming pools")
private val ASSIGNED_POINTS = listOf(50, 30, 20)

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PrioritizationContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var jdbcTemplate: JdbcTemplate
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

  private fun createQuestionnaire() =
      questionnaireService.create(
          ROOM_ID, "Priority ${UUID.randomUUID()}", QuestionnaireType.SURVEY)

  private fun createPrioritizationContent(options: List<String> = OPTIONS): Content =
      contentService.create(
          createQuestionnaire().id!!,
          BODY,
          PrioritizationContentFormatData(options),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answer(content: Content, assignedPoints: List<Int>): Answer {
    authenticate(userService.createAccount().id!!)
    try {
      return answerService.createAnswer(
          content.activeRound!!.id!!, PrioritizationAnswerFormatData(assignedPoints))
    } finally {
      authenticate(OWNER_ID)
    }
  }

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) =
      reload(content).formatData as PrioritizationContentFormatData

  @Test
  fun shouldPersistAndReloadPrioritizationFormat() {
    val content = createPrioritizationContent()
    val formatData = formatDataOf(content)
    Assertions.assertEquals("PRIORITIZATION", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertEquals(OPTIONS, formatData.options)
  }

  @Test
  fun shouldExposeAssignablePointsWithoutPersistingIt() {
    val content = createPrioritizationContent()
    Assertions.assertEquals(
        PrioritizationContentFormatData.ASSIGNABLE_POINTS, formatDataOf(content).assignablePoints)
    val storedFormatData =
        jdbcTemplate.queryForObject(
            "SELECT format_data::text FROM questionnaire.content WHERE id = ?",
            String::class.java,
            content.id)
    Assertions.assertFalse(storedFormatData!!.contains("assignablePoints"))
  }

  @Test
  fun shouldPersistAndReloadPrioritizationAnswerFormat() {
    val answer = answer(createPrioritizationContent(), ASSIGNED_POINTS)
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("PRIORITIZATION", reloaded.formatData!!.format)
    Assertions.assertEquals(
        ASSIGNED_POINTS, (reloaded.formatData as PrioritizationAnswerFormatData).assignedPoints)
  }

  @Test
  fun shouldScoreAnswerAsNeutralWithoutPoints() {
    val answer = answer(createPrioritizationContent(), ASSIGNED_POINTS)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldAcceptAnswerGivingEverythingToOneOption() {
    val answer =
        answer(
            createPrioritizationContent(),
            listOf(PrioritizationContentFormatData.ASSIGNABLE_POINTS, 0, 0))
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
  }

  @Test
  fun shouldCreateAbstention() {
    val content = createPrioritizationContent()
    val answer = answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldRejectContentWithTooFewOptions() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createPrioritizationContent(listOf("Bicycle lanes"))
    }
  }

  @Test
  fun shouldRejectBlankOption() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createPrioritizationContent(listOf("Bicycle lanes", " "))
    }
  }

  @Test
  fun shouldRejectRepeatedOption() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createPrioritizationContent(listOf("Bicycle lanes", "Bicycle lanes"))
    }
  }

  @Test
  fun shouldRejectAnswerWithWrongNumberOfEntries() {
    val content = createPrioritizationContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(60, 40))
    }
  }

  @Test
  fun shouldRejectAnswerWithEntryOutOfRange() {
    val content = createPrioritizationContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(150, -50, 0))
    }
  }

  @Test
  fun shouldRejectAnswerNotAssigningAllPoints() {
    val content = createPrioritizationContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(50, 30, 10))
    }
  }

  @Test
  fun shouldRejectAnswerAssigningTooManyPoints() {
    val content = createPrioritizationContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, listOf(50, 30, 40))
    }
  }

  @Test
  fun shouldUpdateOptionsWithoutAnswers() {
    val content = createPrioritizationContent()
    contentService.update(
        content.id!!, BODY, PrioritizationContentFormatData(OTHER_OPTIONS), null, true, null)
    Assertions.assertEquals(OTHER_OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectOptionChangeOnAnsweredContent() {
    val content = createPrioritizationContent()
    answer(content, ASSIGNED_POINTS)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, PrioritizationContentFormatData(OTHER_OPTIONS), null, true, null)
    }
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectOptionReorderingOnAnsweredContent() {
    val content = createPrioritizationContent()
    answer(content, ASSIGNED_POINTS)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, PrioritizationContentFormatData(OPTIONS.reversed()), null, true, null)
    }
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldUpdateUnrelatedFieldsOnAnsweredContent() {
    val content = createPrioritizationContent()
    answer(content, ASSIGNED_POINTS)
    contentService.update(
        content.id!!,
        OTHER_BODY,
        PrioritizationContentFormatData(OPTIONS),
        DURATION,
        false,
        null,
    )
    val reloaded = reload(content)
    Assertions.assertEquals(OTHER_BODY, reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
    Assertions.assertEquals(OPTIONS, formatDataOf(content).options)
  }

  @Test
  fun shouldRejectPrioritizationUpdateForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, PrioritizationContentFormatData(OPTIONS), null, true, null)
    }
  }

  @Test
  fun shouldRejectPrioritizationAnswerForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answer(content, ASSIGNED_POINTS)
    }
  }

  private fun createTextContent(): Content =
      contentService.create(
          createQuestionnaire().id!!,
          BODY,
          TextContentFormatData(false),
          null,
          true,
          null,
      )
}
