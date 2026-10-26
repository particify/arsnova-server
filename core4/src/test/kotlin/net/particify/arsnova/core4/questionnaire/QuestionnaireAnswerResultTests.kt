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
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardAnswerProjection
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val OTHER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val OPTIONS = listOf("Right", "Wrong")
private const val CORRECT_INDEX = 0
private const val WRONG_INDEX = 1

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireAnswerResultTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
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

  /** A survey leaves the round open, unlike a quiz, even for a content that can be scored. */
  private fun createChoiceContent(correctOptionIndexes: List<Int>?): Content {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Scoring ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    return contentService.create(
        questionnaire.id!!,
        "Pick one",
        ChoiceContentFormatData(OPTIONS, correctOptionIndexes, false),
        null,
        true,
        null,
    )
  }

  private fun answer(content: Content, index: Int) =
      answerService.createAnswer(content.activeRound!!.id!!, ChoiceAnswerFormatData(listOf(index)))

  private fun resultsOf(content: Content) =
      answerService.findQuestionnaireAnswerResults(content.questionnaire!!.id!!, OWNER_ID)

  @Test
  fun shouldCountCorrectAnswerOnceWithItsPoints() {
    val content = createChoiceContent(listOf(CORRECT_INDEX))
    val answer = answer(content, CORRECT_INDEX)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
    val results = resultsOf(content)
    Assertions.assertEquals(1, results.scorableContentCount)
    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(Content.BASE_POINTS, results.achievedScore)
    Assertions.assertEquals(Content.BASE_POINTS, results.maxScore)
  }

  @Test
  fun shouldNotCountWrongAnswer() {
    val content = createChoiceContent(listOf(CORRECT_INDEX))
    val answer = answer(content, WRONG_INDEX)
    Assertions.assertEquals(AnswerResultState.WRONG, answer.result)
    Assertions.assertNull(answer.points)
    val results = resultsOf(content)
    Assertions.assertEquals(1, results.scorableContentCount)
    Assertions.assertEquals(0, results.correctAnswerCount)
    Assertions.assertEquals(0, results.achievedScore)
  }

  @Test
  fun shouldNotCountNeutralAnswerOfContentWithoutCorrectOption() {
    val content = createChoiceContent(null)
    val answer = answer(content, CORRECT_INDEX)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
    val results = resultsOf(content)
    Assertions.assertEquals(0, results.scorableContentCount)
    Assertions.assertEquals(0, results.correctAnswerCount)
    Assertions.assertEquals(0, results.achievedScore)
    Assertions.assertEquals(0, results.maxScore)
  }

  @Test
  fun shouldServeTheScorableContentCountThroughTheSchema() {
    val content = createChoiceContent(listOf(CORRECT_INDEX))
    answer(content, CORRECT_INDEX)
    val response =
        graphQlService.executeAsCurrentUser(
            """
            {
              questionnaireAnswerResultsByQuestionnaireId(
                questionnaireId: "${content.questionnaire!!.id}"
              ) {
                scorableContentCount
                maxScore
              }
            }
            """
                .trimIndent())
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    val result =
        response
            .getData<Map<String, Map<String, Int?>>>()!!
            .getValue("questionnaireAnswerResultsByQuestionnaireId")
    Assertions.assertEquals(1, result["scorableContentCount"])
    Assertions.assertEquals(Content.BASE_POINTS, result["maxScore"])
  }

  @Test
  fun shouldNotCountStoredAnswerWithZeroPointsAndNonCorrectResult() {
    val content = createChoiceContent(listOf(CORRECT_INDEX))
    answerRepository.save(
        Answer(
            round = content.activeRound,
            formatData = ChoiceAnswerFormatData(listOf(WRONG_INDEX)),
            result = AnswerResultState.WRONG,
            points = 0,
        ))
    val results = resultsOf(content)
    Assertions.assertEquals(0, results.correctAnswerCount)
    Assertions.assertEquals(0, results.achievedScore)
  }

  /** The leaderboard reads a projection instead of the answers, which must not change its data. */
  @Test
  fun shouldProjectTheAnswersAsTheLeaderboardReadsThem() {
    val content = createChoiceContent(listOf(CORRECT_INDEX))
    answer(content, CORRECT_INDEX)
    val roundId = content.activeRound!!.id!!
    val fromEntities =
        answerRepository.findByRoundId(roundId).map {
          LeaderboardAnswerProjection(
              it.auditMetadata.createdBy!!, it.points, it.durationMs, it.result)
        }
    val projected = answerRepository.findLeaderboardProjectionsByRoundId(roundId)
    Assertions.assertEquals(
        listOf(Triple(OWNER_ID, Content.BASE_POINTS, AnswerResultState.CORRECT)),
        projected.map { Triple(it.userId, it.points, it.result) })
    Assertions.assertEquals(fromEntities, projected)
  }

  /** Stores a scored answer as the authenticated user, as given before any rule could refuse it. */
  private fun storeAnswer(round: Round, points: Int, hidden: Boolean = false) =
      answerRepository.saveAndFlush(
          Answer(
              round = round,
              formatData = ChoiceAnswerFormatData(listOf(CORRECT_INDEX)),
              result = AnswerResultState.CORRECT,
              points = points,
              hidden = hidden))

  /**
   * The leaderboard ranks the active round of each content only, so a new round starts it over, and
   * an answer hidden from the statistics still scores.
   */
  @Test
  fun shouldSumTheLeaderboardScoresOfTheActiveRoundsPerUser() {
    val first = createChoiceContent(listOf(CORRECT_INDEX))
    val questionnaireId = first.questionnaire!!.id!!
    val second =
        contentService.create(
            questionnaireId,
            "Pick another",
            ChoiceContentFormatData(OPTIONS, listOf(CORRECT_INDEX), false),
            null,
            true,
            null)
    storeAnswer(first.activeRound!!, points = 100)
    val secondRound = roundService.startNewRound(first.id!!)
    storeAnswer(secondRound, points = 7)
    authenticate(OTHER_USER_ID)
    storeAnswer(secondRound, points = 5, hidden = true)
    storeAnswer(second.activeRound!!, points = 3)

    val sums =
        answerRepository.sumPointsByQuestionnaireIdGroupByCreatedBy(questionnaireId).associate {
          it.get(0, UUID::class.java) to (it.get(1) as Number?)?.toLong()
        }

    Assertions.assertEquals(mapOf(OWNER_ID to 7L, OTHER_USER_ID to 8L), sums)
  }
}
