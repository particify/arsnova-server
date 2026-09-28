/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
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
private val TEMPLATE = ScaleContentFormatData.Template.AGREEMENT

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ScaleRoundStatsTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundStatsService: RoundStatsServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(OWNER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createContent(neutralOption: Boolean = true): Content {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Scale stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    return contentService.create(
        questionnaire.id!!,
        "How much do you agree?",
        ScaleContentFormatData(TEMPLATE, neutralOption),
        null,
        true,
        null,
    )
  }

  private fun answerAll(content: Content, ratings: List<Int>) {
    ratings.forEach {
      answerService.createAnswer(content.activeRound!!.id!!, ScaleAnswerFormatData(it))
    }
  }

  private fun statsOf(content: Content) =
      roundStatsService.getScaleRoundStatsByRoundId(content.activeRound!!.id!!)

  @Test
  fun shouldCountRatingsPerOptionZeroFilled() {
    val content = createContent()
    answerAll(content, listOf(0, 2, 2, 4))
    Assertions.assertEquals(listOf(1, 0, 2, 0, 1), statsOf(content).counts)
  }

  @Test
  fun shouldCountFourOptionsWithoutNeutralOption() {
    val content = createContent(neutralOption = false)
    answerAll(content, listOf(1, 3, 3))
    Assertions.assertEquals(listOf(0, 1, 0, 2), statsOf(content).counts)
  }

  @Test
  fun shouldReturnZeroCountsWithoutAnswers() {
    Assertions.assertEquals(listOf(0, 0, 0, 0, 0), statsOf(createContent()).counts)
  }

  @Test
  fun shouldIgnoreAbstentions() {
    val content = createContent()
    answerAll(content, listOf(1))
    answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(listOf(0, 1, 0, 0, 0), statsOf(content).counts)
  }

  @Test
  fun shouldRejectStatsRequestForNonScaleContent() {
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
