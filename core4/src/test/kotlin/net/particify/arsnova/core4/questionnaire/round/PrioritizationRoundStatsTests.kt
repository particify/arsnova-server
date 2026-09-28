/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val OPTIONS = listOf("Bicycle lanes", "Public transport", "Car parking")
private val BALANCED_POINTS = listOf(50, 30, 20)
private val FOCUSED_POINTS = listOf(0, 100, 0)

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PrioritizationRoundStatsTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundStatsService: RoundStatsServiceImpl
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
          ROOM_ID, "Prio stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)

  private fun createContent(): Content =
      contentService.create(
          createQuestionnaire().id!!,
          "How should the budget be split?",
          PrioritizationContentFormatData(OPTIONS),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answerAll(content: Content, assignedPoints: List<List<Int>>): List<Answer> {
    val answers =
        assignedPoints.map {
          authenticate(userService.createAccount().id!!)
          answerService.createAnswer(content.activeRound!!.id!!, PrioritizationAnswerFormatData(it))
        }
    authenticate(OWNER_ID)
    return answers
  }

  private fun statsOf(content: Content) =
      roundStatsService.getPrioritizationRoundStatsByRoundId(content.activeRound!!.id!!)

  @Test
  fun shouldReturnZeroPerOptionWithoutAnswers() {
    Assertions.assertEquals(listOf(0, 0, 0), statsOf(createContent()).assignedPoints)
  }

  @Test
  fun shouldSumAssignedPointsPerOption() {
    val content = createContent()
    answerAll(content, listOf(BALANCED_POINTS, FOCUSED_POINTS))
    Assertions.assertEquals(listOf(50, 130, 20), statsOf(content).assignedPoints)
  }

  @Test
  fun shouldIgnoreAbstentions() {
    val content = createContent()
    answerAll(content, listOf(BALANCED_POINTS))
    answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(BALANCED_POINTS, statsOf(content).assignedPoints)
  }

  @Test
  fun shouldTreatStoredEmptyPointsAsAbstention() {
    val content = createContent()
    answerAll(content, listOf(BALANCED_POINTS))
    answerRepository.save(
        Answer(
            round = content.activeRound,
            result = AnswerResultState.NEUTRAL,
            formatData = PrioritizationAnswerFormatData(emptyList()),
        ))
    Assertions.assertEquals(BALANCED_POINTS, statsOf(content).assignedPoints)
  }

  @Test
  fun shouldRejectStatsRequestForOtherFormat() {
    val content =
        contentService.create(
            createQuestionnaire().id!!,
            "Open question",
            TextContentFormatData(false),
            null,
            true,
            null,
        )
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { statsOf(content) }
  }
}
