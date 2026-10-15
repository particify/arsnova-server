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
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.user.User
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

/** A participant of [ROOM_ID] in the `dev` Liquibase fixtures. */
private val PARTICIPANT_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val TERMS = listOf("Frankfurt am Main")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ShortAnswerRoundStatsTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var membershipService: MembershipServiceImpl
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
      roundStatsService.getShortAnswerRoundStatsByRoundId(
          content.activeRound!!.id!!, SecurityContextHolder.getContext().authentication)

  private fun createEditor(): UUID {
    val user = userService.createAccount()
    membershipService.save(
        Membership(room = Room(id = ROOM_ID), user = User(id = user.id), role = RoomRole.EDITOR))
    return user.id!!
  }

  private fun publishSolution(content: Content) {
    authenticate(OWNER_ID)
    questionnaireService.update(content.questionnaire!!.id!!, null, null, null, null, true, null)
  }

  private fun textsAs(userId: UUID, content: Content): Any? {
    authenticate(userId)
    val response =
        graphQlService.executeAsCurrentUser(
            """
            {
              questionnaireShortAnswerRoundStatsByRoundId(roundId: "${content.activeRound!!.id}") {
                texts
              }
            }
            """)
    Assertions.assertTrue(response.errors.isEmpty()) { response.errors.toString() }
    val data = response.getData<Map<String, Map<String, Any?>>>()!!
    return data.getValue("questionnaireShortAnswerRoundStatsByRoundId")["texts"]
  }

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
  fun shouldLabelCorrectTermGroupForParticipantOnlyOnceTheSolutionIsPublished() {
    val content = createContent()
    questionnaireService.updatePublished(content.questionnaire!!.id!!, true)
    answerAll(content, listOf("frankfurt am main", "FRANKFURT AM MAIN", "frankfurt am main"))
    val editorId = createEditor()
    Assertions.assertEquals(listOf("frankfurt am main"), textsAs(PARTICIPANT_ID, content))
    Assertions.assertEquals(listOf("Frankfurt am Main"), textsAs(editorId, content))
    publishSolution(content)
    Assertions.assertEquals(listOf("Frankfurt am Main"), textsAs(PARTICIPANT_ID, content))
    Assertions.assertEquals(listOf("Frankfurt am Main"), textsAs(editorId, content))
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
