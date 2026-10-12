/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
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
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30

/** The instants a round records over its life, and what they mean for answers and clients. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoundLifecycleTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(USER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Lifecycle ${UUID.randomUUID()}", type)

  private fun createChoiceContent(questionnaireId: UUID, duration: Int?): Content =
      contentService.create(
          questionnaireId,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          duration,
          true,
          null)

  private fun startLiveContent(contentId: UUID): Round =
      publishingService.startLiveContent(contentId).activeRound!!

  private fun expire(round: Round): Round {
    round.answeringEndTime = Instant.now().minus(1, ChronoUnit.MINUTES)
    return roundService.save(round)
  }

  private fun reload(round: Round): Round = roundService.findById(round.id!!).orElseThrow()

  @Test
  fun shouldScoreNoPointsForAnswersGivenAfterTheCountdownRanOut() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = expire(startLiveContent(content.id!!))
    publishingService.stopLiveContent(questionnaire.id!!)
    val reopened = startLiveContent(content.id!!)
    Assertions.assertEquals(
        round.answeringEndTime!!.truncatedTo(ChronoUnit.MILLIS),
        reopened.answeringExpiredAt!!.truncatedTo(ChronoUnit.MILLIS))
    val answer = answerService.createAnswer(reopened.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertNull(answer.points)
    val results =
        answerService.findQuestionnaireAnswerResults(
            questionnaire.id!!, USER_ID, ScrollSubrange.create(null, null, true))
    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(0, results.achievedScore)
    Assertions.assertEquals(Content.BASE_POINTS, results.maxScore)
  }

  private fun startedThroughSchema(content: Content): Boolean {
    val response =
        graphQlService.executeAsCurrentUser(
            """
            {
              questionnaireRoundsByContentId(contentId: "${content.id}") {
                started
              }
            }
            """
                .trimIndent())
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response
        .getData<Map<String, List<Map<String, Boolean>>>>()!!
        .getValue("questionnaireRoundsByContentId")
        .single()
        .getValue("started")
  }

  @Test
  fun shouldReportWhetherARoundHasEverStarted() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val presented = createChoiceContent(questionnaire.id!!, DURATION)
    val waiting = createChoiceContent(questionnaire.id!!, DURATION)
    startLiveContent(presented.id!!)
    publishingService.stopLiveContent(questionnaire.id!!)
    Assertions.assertTrue(reload(presented.activeRound!!).closed)
    Assertions.assertTrue(startedThroughSchema(presented))
    Assertions.assertFalse(startedThroughSchema(waiting))
    val survey = createQuestionnaire(QuestionnaireType.SURVEY)
    Assertions.assertTrue(startedThroughSchema(createChoiceContent(survey.id!!, null)))
  }

  @Test
  fun shouldKeepScoringAfterAManualStop() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    startLiveContent(content.id!!)
    publishingService.stopLiveContent(questionnaire.id!!)
    val reopened = startLiveContent(content.id!!)
    Assertions.assertNull(reopened.answeringExpiredAt)
    val answer = answerService.createAnswer(reopened.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
  }
}
