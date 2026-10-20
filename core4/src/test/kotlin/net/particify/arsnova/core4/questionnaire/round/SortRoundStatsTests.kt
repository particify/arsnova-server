/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
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
private val OPTIONS = listOf("Hamburg", "Frankfurt am Main", "Munich")
private val CORRECT_ORDER = listOf(0, 1, 2)
private val PERMUTED_ORDER = listOf(1, 0, 2)

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class SortRoundStatsTests {
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
          ROOM_ID, "Sort stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)

  private fun createContent(): Content =
      contentService.create(
          createQuestionnaire().id!!,
          "Order these cities from north to south.",
          SortContentFormatData(OPTIONS),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answerAll(content: Content, orders: List<List<Int>>): List<Answer> {
    val answers =
        orders.map {
          authenticate(userService.createAccount().id!!)
          answerService.createAnswer(content.activeRound!!.id!!, SortAnswerFormatData(it))
        }
    authenticate(OWNER_ID)
    return answers
  }

  private fun statsOf(content: Content) =
      roundStatsService.getSortRoundStatsByRoundId(content.activeRound!!.id!!)

  @Test
  fun shouldReturnEmptyStatsWithoutAnswers() {
    Assertions.assertEquals(emptyList<SortCombination>(), statsOf(createContent()).combinations)
  }

  @Test
  fun shouldGroupIdenticalOrders() {
    val content = createContent()
    answerAll(content, listOf(PERMUTED_ORDER, PERMUTED_ORDER))
    Assertions.assertEquals(
        listOf(SortCombination(PERMUTED_ORDER, 2)), statsOf(content).combinations)
  }

  @Test
  fun shouldKeepDifferentOrdersSeparateAndSortByCount() {
    val content = createContent()
    answerAll(content, listOf(CORRECT_ORDER, PERMUTED_ORDER, PERMUTED_ORDER))
    Assertions.assertEquals(
        listOf(SortCombination(PERMUTED_ORDER, 2), SortCombination(CORRECT_ORDER, 1)),
        statsOf(content).combinations)
  }

  @Test
  fun shouldIgnoreAbstentions() {
    val content = createContent()
    answerAll(content, listOf(CORRECT_ORDER))
    answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(
        listOf(SortCombination(CORRECT_ORDER, 1)), statsOf(content).combinations)
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
