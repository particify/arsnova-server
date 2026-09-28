/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.executeAsCurrentUser
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
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val TERMS = listOf("Frankfurt am Main")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ShortAnswerRoundStatsTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
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
          ROOM_ID, "Short stats ${UUID.randomUUID()}", QuestionnaireType.SURVEY)

  private fun createContent(correctTerms: List<String> = TERMS): Content =
      contentService.create(
          createQuestionnaire().id!!,
          "Which city hosts the ARS conference?",
          ShortAnswerContentFormatData(correctTerms),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answerAll(content: Content, bodies: List<String>): List<Answer> {
    val answers =
        bodies.map {
          authenticate(userService.createAccount().id!!)
          answerService.createAnswer(content.activeRound!!.id!!, ShortAnswerAnswerFormatData(it))
        }
    authenticate(OWNER_ID)
    return answers
  }

  private fun statsOf(content: Content) =
      roundStatsService.getShortAnswerRoundStatsByRoundId(content.activeRound!!.id!!)

  @Test
  fun shouldReturnEmptyStatsWithoutAnswers() {
    val stats = statsOf(createContent())
    Assertions.assertEquals(emptyList<String>(), stats.texts)
    Assertions.assertEquals(emptyList<Int>(), stats.counts)
  }

  @Test
  fun shouldCountAnswersDescendingByCount() {
    val content = createContent()
    answerAll(content, listOf("Offenbach", "Hanau", "Offenbach"))
    val stats = statsOf(content)
    Assertions.assertEquals(listOf("Offenbach", "Hanau"), stats.texts)
    Assertions.assertEquals(listOf(2, 1), stats.counts)
  }

  @Test
  fun shouldGroupAnswersDifferingOnlyInCaseOrPunctuation() {
    val content = createContent()
    answerAll(content, listOf("Offenbach", "offenbach", "OFFENBACH!"))
    val stats = statsOf(content)
    Assertions.assertEquals(listOf("Offenbach"), stats.texts)
    Assertions.assertEquals(listOf(3), stats.counts)
  }

  @Test
  fun shouldLabelCorrectTermGroupWithItsOwnSpelling() {
    val content = createContent()
    answerAll(content, listOf("frankfurt am main", "FRANKFURT AM MAIN", "frankfurt-am-main"))
    Assertions.assertEquals(listOf("Frankfurt am Main"), statsOf(content).texts)
  }

  @Test
  fun shouldIgnoreAbstentions() {
    val content = createContent()
    answerAll(content, listOf("Offenbach"))
    answerService.createAbstention(content.activeRound!!.id!!)
    val stats = statsOf(content)
    Assertions.assertEquals(listOf("Offenbach"), stats.texts)
    Assertions.assertEquals(listOf(1), stats.counts)
  }

  @Test
  fun shouldExcludeHiddenAnswers() {
    val content = createContent()
    val answers = answerAll(content, listOf("Offenbach", "Hanau"))
    answerService.updateHidden(answers.first().id!!, true)
    val stats = statsOf(content)
    Assertions.assertEquals(listOf("Hanau"), stats.texts)
    Assertions.assertEquals(listOf(1), stats.counts)
  }

  @Test
  fun shouldExcludeAnswersMatchingABannedKeywordThroughGraphQl() {
    val content = createContent()
    answerAll(content, listOf("Offenbach", "Spam", "spam!", "Offenbach am Main"))
    val ban =
        graphQlService.executeAsCurrentUser(
            """
            mutation {
              banQuestionnaireShortAnswerContentKeyword(contentId: "${content.id}", keyword: "SPAM") {
                id
              }
            }
            """)
    Assertions.assertTrue(ban.errors.isEmpty()) { ban.errors.toString() }
    val stats =
        graphQlService.executeAsCurrentUser(
            """
            {
              questionnaireShortAnswerRoundStatsByRoundId(roundId: "${content.activeRound!!.id}") {
                texts
                counts
              }
            }
            """)
    Assertions.assertTrue(stats.errors.isEmpty()) { stats.errors.toString() }
    Assertions.assertEquals(
        mapOf(
            "questionnaireShortAnswerRoundStatsByRoundId" to
                mapOf(
                    "texts" to listOf("Offenbach", "Offenbach am Main"), "counts" to listOf(1, 1))),
        stats.getData<Map<String, Any?>>())
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
