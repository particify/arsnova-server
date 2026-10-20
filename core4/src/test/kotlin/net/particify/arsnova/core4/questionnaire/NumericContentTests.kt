/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
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
private const val DURATION = 30
private const val MIN_NUMBER = 1.0
private const val MAX_NUMBER = 100.0
private const val CORRECT_NUMBER = 42.0
private const val TOLERANCE = 2.5

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class NumericContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundService: RoundServiceImpl
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
      questionnaireService.create(ROOM_ID, "Numeric ${UUID.randomUUID()}", type)

  private fun createNumericContent(
      questionnaireId: UUID,
      correctNumber: Double? = null,
      tolerance: Double = 0.0,
      duration: Int? = null
  ): Content =
      contentService.create(
          questionnaireId,
          "Guess the number",
          NumericContentFormatData(MIN_NUMBER, MAX_NUMBER, correctNumber, tolerance),
          duration,
          true,
          null,
      )

  /** Creates a content with a correct answer on an open round of a survey. */
  private fun createScorableContent(tolerance: Double = 0.0): Content =
      createNumericContent(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!, CORRECT_NUMBER, tolerance)

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answer(content: Content, selectedNumber: Double): Answer {
    authenticate(userService.createAccount().id!!)
    try {
      return answerService.createAnswer(
          content.activeRound!!.id!!, NumericAnswerFormatData(selectedNumber))
    } finally {
      authenticate(OWNER_ID)
    }
  }

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun expire(round: Round): Round {
    round.answeringEndTime = Instant.now().minus(1, ChronoUnit.MINUTES)
    return roundService.save(round)
  }

  @Test
  fun shouldPersistAndReloadNumericFormat() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createNumericContent(questionnaire.id!!, CORRECT_NUMBER, TOLERANCE)
    val reloaded = reload(content)
    val formatData = reloaded.formatData as NumericContentFormatData
    Assertions.assertEquals("NUMERIC", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertEquals(MIN_NUMBER, formatData.minNumber)
    Assertions.assertEquals(MAX_NUMBER, formatData.maxNumber)
    Assertions.assertEquals(CORRECT_NUMBER, formatData.correctNumber)
    Assertions.assertEquals(TOLERANCE, formatData.tolerance)
    Assertions.assertEquals(Content.BASE_POINTS, reloaded.points)
  }

  @Test
  fun shouldPersistAndReloadNumericFormatWithoutCorrectNumber() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createNumericContent(questionnaire.id!!)
    val formatData = reload(content).formatData as NumericContentFormatData
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertNull(formatData.correctNumber)
    Assertions.assertEquals(0.0, formatData.tolerance)
  }

  @Test
  fun shouldPersistAndReloadNumericAnswerFormat() {
    val content = createScorableContent()
    val answer = answer(content, CORRECT_NUMBER)
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("NUMERIC", reloaded.formatData!!.format)
    Assertions.assertEquals(
        CORRECT_NUMBER, (reloaded.formatData as NumericAnswerFormatData).selectedNumber)
  }

  @Test
  fun shouldAcceptExactMatchWithoutTolerance() {
    val content = createScorableContent()
    val answer = answer(content, CORRECT_NUMBER)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
  }

  @Test
  fun shouldRejectNearMissWithoutTolerance() {
    val content = createScorableContent()
    val answer = answer(content, CORRECT_NUMBER + 1)
    Assertions.assertEquals(AnswerResultState.WRONG, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldAcceptAnswerAtToleranceBounds() {
    val content = createScorableContent(TOLERANCE)
    Assertions.assertEquals(
        AnswerResultState.CORRECT, answer(content, CORRECT_NUMBER - TOLERANCE).result)
    Assertions.assertEquals(
        AnswerResultState.CORRECT, answer(content, CORRECT_NUMBER + TOLERANCE).result)
  }

  @Test
  fun shouldRejectAnswerOutsideToleranceBounds() {
    val content = createScorableContent(TOLERANCE)
    Assertions.assertEquals(
        AnswerResultState.WRONG, answer(content, CORRECT_NUMBER - TOLERANCE - 0.1).result)
    Assertions.assertEquals(
        AnswerResultState.WRONG, answer(content, CORRECT_NUMBER + TOLERANCE + 0.1).result)
  }

  @Test
  fun shouldCreateNeutralAnswerWithoutCorrectNumber() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createNumericContent(questionnaire.id!!)
    val answer = answer(content, CORRECT_NUMBER)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldCreateAbstention() {
    val content = createScorableContent()
    val answer = answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldRejectNumericAnswerOnClosedRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createNumericContent(questionnaire.id!!, CORRECT_NUMBER)
    Assertions.assertThrows(RoundClosedException::class.java) { answer(content, CORRECT_NUMBER) }
  }

  @Test
  fun shouldRejectNumericAnswerAfterDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createNumericContent(questionnaire.id!!, CORRECT_NUMBER, duration = DURATION)
    val round = expire(publishingService.startLiveContent(content.id!!).activeRound!!)
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAnswer(round.id!!, NumericAnswerFormatData(CORRECT_NUMBER))
    }
  }
}
