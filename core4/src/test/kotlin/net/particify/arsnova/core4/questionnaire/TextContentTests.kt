/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.api.ContentQueryController
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundExpiredException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
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
private val PARTICIPANT_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private const val DURATION = 30

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class TextContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentQueryController: ContentQueryController
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    authenticateAs(OWNER_ID)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticateAs(userId: UUID) {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Text ${UUID.randomUUID()}", type)

  private fun createTextContent(questionnaireId: UUID, duration: Int? = null): Content =
      contentService.create(
          questionnaireId,
          "Open question",
          TextContentFormatData(false),
          duration,
          true,
          null,
      )

  private fun createChoiceContent(questionnaireId: UUID): Content =
      contentService.create(
          questionnaireId,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null,
      )

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) = reload(content).formatData as TextContentFormatData

  private fun expire(round: Round): Round {
    round.answeringEndTime = Instant.now().minus(1, ChronoUnit.MINUTES)
    return roundService.save(round)
  }

  @Test
  fun shouldPersistAndReloadTextFormat() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val reloaded = reload(content)
    Assertions.assertEquals("TEXT", reloaded.formatData!!.format)
    Assertions.assertTrue(reloaded.formatData!!.answerable())
    Assertions.assertFalse((reloaded.formatData as TextContentFormatData).answersPublished)
  }

  @Test
  fun shouldPersistAndReloadTextAnswerFormat() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val answer =
        answerService.createAnswer(
            content.activeRound!!.id!!, TextAnswerFormatData("An open answer"))
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("TEXT", reloaded.formatData!!.format)
    Assertions.assertEquals("An open answer", (reloaded.formatData as TextAnswerFormatData).body)
  }

  @Test
  fun shouldPreserveAnswersPublishedWhenUpdatingContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    contentService.updateTextAnswersPublished(content.id!!, true)
    contentService.update(
        content.id!!, "Reworded question", TextContentFormatData(false), DURATION, false, null)
    val reloaded = reload(content)
    Assertions.assertEquals("Reworded question", reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
    Assertions.assertTrue((reloaded.formatData as TextContentFormatData).answersPublished)
  }

  @Test
  fun shouldToggleAnswersPublished() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    Assertions.assertFalse(formatDataOf(content).answersPublished)
    contentService.updateTextAnswersPublished(content.id!!, true)
    Assertions.assertTrue(formatDataOf(content).answersPublished)
    contentService.updateTextAnswersPublished(content.id!!, false)
    Assertions.assertFalse(formatDataOf(content).answersPublished)
  }

  @Test
  fun shouldRejectAnswerPublishingForNonTextContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createChoiceContent(questionnaire.id!!)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.updateTextAnswersPublished(content.id!!, true)
    }
  }

  @Test
  fun shouldCreateNeutralTextAnswerWithoutPoints() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val answer =
        answerService.createAnswer(content.activeRound!!.id!!, TextAnswerFormatData("Answer"))
    Assertions.assertNotNull(answer.id)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
    Assertions.assertFalse(answer.hidden)
  }

  @Test
  fun shouldRejectTextAnswerOnClosedRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createTextContent(questionnaire.id!!)
    Assertions.assertThrows(RoundClosedException::class.java) {
      answerService.createAnswer(content.activeRound!!.id!!, TextAnswerFormatData("Answer"))
    }
  }

  @Test
  fun shouldRejectTextAnswerAfterDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createTextContent(questionnaire.id!!, DURATION)
    val round = expire(publishingService.startLiveContent(content.id!!).activeRound!!)
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAnswer(round.id!!, TextAnswerFormatData("Answer"))
    }
  }

  @Test
  fun shouldExcludeHiddenAnswerFromRoundQuery() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val roundId = content.activeRound!!.id!!
    val visible = answerService.createAnswer(roundId, TextAnswerFormatData("Visible"))
    authenticateAs(PARTICIPANT_ID)
    val hidden = answerService.createAnswer(roundId, TextAnswerFormatData("Hidden"))
    authenticateAs(OWNER_ID)

    Assertions.assertEquals(
        2, answerRepository.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId).size)
    val toggled = answerService.updateHidden(hidden.id!!, true)
    Assertions.assertTrue(toggled.hidden)
    Assertions.assertEquals(
        listOf(visible.id),
        answerRepository.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId).map { it.id })
    Assertions.assertFalse(answerService.updateHidden(hidden.id!!, false).hidden)
    Assertions.assertEquals(
        2, answerRepository.findByRoundIdAndHiddenFalseOrderByIdAsc(roundId).size)
  }

  @Test
  fun shouldExcludeHiddenAnswerFromAnswerCount() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val answer =
        answerService.createAnswer(content.activeRound!!.id!!, TextAnswerFormatData("Answer"))
    Assertions.assertEquals(1, contentQueryController.answerCount(reload(content)))
    answerService.updateHidden(answer.id!!, true)
    Assertions.assertEquals(0, contentQueryController.answerCount(reload(content)))
  }

  @Test
  fun shouldWithholdAnswersFromParticipantUntilPublished() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val roundId = content.activeRound!!.id!!
    answerService.createAnswer(roundId, TextAnswerFormatData("Answer"))

    authenticateAs(PARTICIPANT_ID)
    Assertions.assertTrue(answerService.findTextAnswersByRoundId(roundId).isEmpty())

    authenticateAs(OWNER_ID)
    contentService.updateTextAnswersPublished(content.id!!, true)

    authenticateAs(PARTICIPANT_ID)
    Assertions.assertEquals(1, answerService.findTextAnswersByRoundId(roundId).size)
  }

  @Test
  fun shouldAlwaysShowAnswersToEditor() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createTextContent(questionnaire.id!!)
    val roundId = content.activeRound!!.id!!
    answerService.createAnswer(roundId, TextAnswerFormatData("Answer"))
    Assertions.assertFalse(formatDataOf(content).answersPublished)
    Assertions.assertEquals(1, answerService.findTextAnswersByRoundId(roundId).size)
    contentService.updateTextAnswersPublished(content.id!!, true)
    Assertions.assertEquals(1, answerService.findTextAnswersByRoundId(roundId).size)
  }
}
