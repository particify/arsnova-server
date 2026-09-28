/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
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
private const val MIN_NUMBER = 0.0
private const val MAX_NUMBER = 100.0
private const val DELTA = 1e-9

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class NumericRoundStatsTests {
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

  private fun createContent(correctNumber: Double? = null, tolerance: Double = 0.0): Content {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Numeric stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    return contentService.create(
        questionnaire.id!!,
        "Guess the number",
        NumericContentFormatData(MIN_NUMBER, MAX_NUMBER, correctNumber, tolerance),
        null,
        true,
        null,
    )
  }

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answerAll(content: Content, numbers: List<Double>) {
    numbers.forEach {
      authenticate(userService.createAccount().id!!)
      answerService.createAnswer(content.activeRound!!.id!!, NumericAnswerFormatData(it))
    }
    authenticate(OWNER_ID)
  }

  private fun statsOf(content: Content) =
      roundStatsService.getNumericRoundStatsByRoundId(content.activeRound!!.id!!)

  @Test
  fun shouldAggregateSelectedNumbersAscendingWithCounts() {
    val content = createContent()
    answerAll(content, listOf(5.0, 2.0, 1.0, 2.0))
    val stats = statsOf(content)
    Assertions.assertEquals(listOf(1.0, 2.0, 5.0), stats.selectedNumbers)
    Assertions.assertEquals(listOf(1, 2, 1), stats.counts)
  }

  @Test
  fun shouldComputeDescriptiveStatistics() {
    val content = createContent()
    answerAll(content, listOf(1.0, 2.0, 2.0, 5.0))
    val stats = statsOf(content)
    Assertions.assertEquals(2.5, stats.mean, DELTA)
    Assertions.assertEquals(2.0, stats.median, DELTA)
    Assertions.assertEquals(2.25, stats.variance, DELTA)
    Assertions.assertEquals(1.5, stats.standardDeviation, DELTA)
    Assertions.assertEquals(1.0, stats.minimum, DELTA)
    Assertions.assertEquals(5.0, stats.maximum, DELTA)
  }

  @Test
  fun shouldComputeMedianOfMiddleValueForOddAnswerCount() {
    val content = createContent()
    answerAll(content, listOf(5.0, 1.0, 2.0))
    Assertions.assertEquals(2.0, statsOf(content).median, DELTA)
  }

  @Test
  fun shouldComputeCorrectAnswerFractionWithoutTolerance() {
    val content = createContent(correctNumber = 2.0)
    answerAll(content, listOf(1.0, 2.0, 2.0, 5.0))
    Assertions.assertEquals(0.5, statsOf(content).correctAnswerFraction!!, DELTA)
  }

  @Test
  fun shouldComputeCorrectAnswerFractionWithinTolerance() {
    val content = createContent(correctNumber = 2.0, tolerance = 1.0)
    answerAll(content, listOf(1.0, 2.0, 2.0, 5.0))
    Assertions.assertEquals(0.75, statsOf(content).correctAnswerFraction!!, DELTA)
  }

  @Test
  fun shouldOmitCorrectAnswerFractionWithoutCorrectNumber() {
    val content = createContent()
    answerAll(content, listOf(1.0, 2.0))
    Assertions.assertNull(statsOf(content).correctAnswerFraction)
  }

  @Test
  fun shouldReturnZeroedStatisticsWithoutAnswers() {
    val content = createContent(correctNumber = 2.0)
    val stats = statsOf(content)
    Assertions.assertTrue(stats.selectedNumbers.isEmpty())
    Assertions.assertTrue(stats.counts.isEmpty())
    Assertions.assertEquals(0.0, stats.mean, DELTA)
    Assertions.assertEquals(0.0, stats.median, DELTA)
    Assertions.assertEquals(0.0, stats.variance, DELTA)
    Assertions.assertEquals(0.0, stats.standardDeviation, DELTA)
    Assertions.assertEquals(0.0, stats.minimum, DELTA)
    Assertions.assertEquals(0.0, stats.maximum, DELTA)
    Assertions.assertEquals(0.0, stats.correctAnswerFraction!!, DELTA)
  }

  @Test
  fun shouldIgnoreAbstentions() {
    val content = createContent()
    answerAll(content, listOf(2.0, 4.0))
    answerService.createAbstention(content.activeRound!!.id!!)
    val stats = statsOf(content)
    Assertions.assertEquals(listOf(2.0, 4.0), stats.selectedNumbers)
    Assertions.assertEquals(listOf(1, 1), stats.counts)
    Assertions.assertEquals(3.0, stats.mean, DELTA)
  }

  @Test
  fun shouldRejectStatsRequestForNonNumericContent() {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Text stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    val content =
        contentService.create(
            questionnaire.id!!,
            "Open question",
            TextContentFormatData(false),
            null,
            true,
            null,
        )
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { statsOf(content) }
  }
}
