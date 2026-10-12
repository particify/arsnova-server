/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.api.AnswerMutationController
import net.particify.arsnova.core4.questionnaire.answer.api.AnswerQueryController
import net.particify.arsnova.core4.questionnaire.answer.api.CreateChoiceAnswerInput
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.content.api.ContentMutationController
import net.particify.arsnova.core4.questionnaire.content.api.ContentQueryController
import net.particify.arsnova.core4.questionnaire.content.api.CreateChoiceContentInput
import net.particify.arsnova.core4.questionnaire.content.api.UpdateChoiceContentInput
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnairePublishingMutationController
import net.particify.arsnova.core4.questionnaire.round.api.RoundMutationController
import net.particify.arsnova.core4.questionnaire.round.api.RoundQueryController
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsSubscriptionController
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.ScrollPosition
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Accounts and rooms from the `dev` Liquibase fixtures. */
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val OTHER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Owned by [OWNER_ID]; [OTHER_USER_ID] is not a member. */
private val FOREIGN_ROOM_ID = UUID.fromString("163bdf7d-a106-4b3d-9d72-00fff4cda386")

/** Owned by [OWNER_ID]; [OTHER_USER_ID] is a participant. */
private val SHARED_ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")

/** Owned by [OTHER_USER_ID]. */
private val OTHER_USERS_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnairePermissionTests {
  @Autowired lateinit var answerMutationController: AnswerMutationController
  @Autowired lateinit var answerQueryController: AnswerQueryController
  @Autowired lateinit var contentMutationController: ContentMutationController
  @Autowired lateinit var contentQueryController: ContentQueryController
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingController: QuestionnairePublishingMutationController
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundMutationController: RoundMutationController
  @Autowired lateinit var roundQueryController: RoundQueryController
  @Autowired lateinit var roundStatsSubscriptionController: RoundStatsSubscriptionController
  @Autowired lateinit var userService: UserService

  private lateinit var foreignQuestionnaire: Questionnaire
  private lateinit var foreignContent: Content
  private lateinit var foreignWordcloud: Content

  private val subrange = ScrollSubrange.create(ScrollPosition.offset(), null, true)

  @BeforeEach
  fun createForeignQuestionnaire() {
    authenticate(OWNER_ID)
    foreignQuestionnaire = createPublishedQuestionnaire(FOREIGN_ROOM_ID)
    foreignContent = createChoiceContent(foreignQuestionnaire)
    foreignWordcloud =
        contentService.create(
            foreignQuestionnaire.id!!, "Wordcloud", WordcloudContentFormatData(), null, true, null)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID): User {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
    return user
  }

  private fun createPublishedQuestionnaire(roomId: UUID): Questionnaire {
    val questionnaire =
        questionnaireService.create(
            roomId, "Permissions ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    return questionnaireService.updatePublished(questionnaire.id!!, true)
  }

  private fun createChoiceContent(
      questionnaire: Questionnaire,
      correctOptionIndexes: List<Int>? = null
  ): Content =
      contentService.create(
          questionnaire.id!!,
          "Choice",
          ChoiceContentFormatData(listOf("A", "B"), correctOptionIndexes, false),
          null,
          true,
          null)

  private fun publishSolution(questionnaire: Questionnaire, published: Boolean) {
    authenticate(OWNER_ID)
    questionnaireService.update(questionnaire.id!!, null, null, null, null, published, null)
  }

  private fun publishStatistics(questionnaire: Questionnaire, published: Boolean) {
    authenticate(OWNER_ID)
    questionnaireService.update(questionnaire.id!!, null, null, null, published, null, null)
  }

  private fun counts(content: Content): List<Int?> =
      listOf(
          contentQueryController.answerCount(content),
          contentQueryController.abstentionCount(content),
          contentQueryController.correctAnswerCount(content))

  private fun roundIdOf(content: Content): UUID = content.activeRound!!.id!!

  private fun assertAllDenied(vararg calls: Executable) {
    calls.forEach { Assertions.assertThrows(AccessDeniedException::class.java, it) }
  }

  @Test
  fun shouldRejectContentOperationsOfNonMember() {
    val contentId = foreignContent.id!!
    val questionnaireId = foreignQuestionnaire.id!!
    authenticate(OTHER_USER_ID)
    val ownQuestionnaire = createPublishedQuestionnaire(OTHER_USERS_ROOM_ID)
    val ownContent = createChoiceContent(ownQuestionnaire)
    assertAllDenied(
        {
          contentMutationController.createQuestionnaireChoiceContent(
              CreateChoiceContentInput(
                  "CHOICE", questionnaireId, "Body", listOf("A"), null, false, null, null, true))
        },
        {
          contentMutationController.updateQuestionnaireChoiceContent(
              UpdateChoiceContentInput(
                  contentId, "Body", listOf("A"), null, false, null, null, true))
        },
        { contentMutationController.duplicateQuestionnaireContent(contentId) },
        {
          contentMutationController.copyQuestionnaireContentToQuestionnaire(
              contentId, ownQuestionnaire.id!!)
        },
        {
          contentMutationController.moveQuestionnaireContentToQuestionnaire(
              contentId, ownQuestionnaire.id!!)
        },
        {
          contentMutationController.moveQuestionnaireContentToQuestionnaire(
              ownContent.id!!, questionnaireId)
        },
        {
          contentMutationController.banQuestionnaireWordcloudContentKeyword(
              foreignWordcloud.id!!, "word")
        },
        {
          contentMutationController.resetBannedQuestionnaireWordcloudContentKeywords(
              foreignWordcloud.id!!)
        },
        { contentQueryController.questionnaireContentById(contentId) },
    )
  }

  @Test
  fun shouldRejectCopyingAContentTheParticipantMayOnlyRead() {
    val questionnaire = createPublishedQuestionnaire(SHARED_ROOM_ID)
    val content = createChoiceContent(questionnaire)
    authenticate(OTHER_USER_ID)
    val ownQuestionnaire = createPublishedQuestionnaire(OTHER_USERS_ROOM_ID)
    Assertions.assertNotNull(contentQueryController.questionnaireContentById(content.id!!))
    assertAllDenied({
      contentMutationController.copyQuestionnaireContentToQuestionnaire(
          content.id!!, ownQuestionnaire.id!!)
    })
  }

  @Test
  fun shouldRejectPublishingAndRoundOperationsOfNonMember() {
    val contentId = foreignContent.id!!
    val questionnaireId = foreignQuestionnaire.id!!
    authenticate(OTHER_USER_ID)
    assertAllDenied(
        { publishingController.startQuestionnaireContent(contentId) },
        { publishingController.stopQuestionnaireContent(questionnaireId) },
        { publishingController.publishQuestionnaireUpToContent(questionnaireId, contentId) },
        { roundMutationController.startQuestionnaireContentRound(contentId) },
        { roundQueryController.questionnaireRoundsByContentId(contentId) },
    )
  }

  @Test
  fun shouldRejectAnswerOperationsOfNonMember() {
    val roundId = roundIdOf(foreignContent)
    val contentId = foreignContent.id!!
    val questionnaireId = foreignQuestionnaire.id!!
    val user = authenticate(OTHER_USER_ID)
    assertAllDenied(
        {
          answerMutationController.createQuestionnaireChoiceAnswer(
              CreateChoiceAnswerInput(roundId, listOf(0)))
        },
        { answerMutationController.createQuestionnaireAbstention(roundId) },
        { answerMutationController.deleteQuestionnaireAnswersByContentId(contentId) },
        { answerQueryController.questionnaireAnswerByRoundId(roundId, user) },
        {
          answerQueryController.questionnaireContentAnswerResultsByQuestionnaireId(
              questionnaireId, user, subrange)
        },
        {
          answerQueryController.questionnaireAnswerResultsByQuestionnaireId(
              questionnaireId, user, subrange)
        },
    )
  }

  @Test
  fun shouldRejectStatisticsOfNonMember() {
    val roundId = roundIdOf(foreignContent)
    val wordcloudRoundId = roundIdOf(foreignWordcloud)
    authenticate(OTHER_USER_ID)
    assertAllDenied(
        { roundQueryController.questionnaireChoiceRoundStatsByRoundId(roundId) },
        { roundQueryController.questionnaireWordcloudRoundStatsByRoundId(wordcloudRoundId) },
        { roundStatsSubscriptionController.questionnaireChoiceRoundStatsUpdated(roundId) },
        {
          roundStatsSubscriptionController.questionnaireWordcloudRoundStatsUpdated(wordcloudRoundId)
        },
    )
  }

  @Test
  fun shouldLetParticipantAnswerAndSeeOwnResults() {
    val questionnaire = createPublishedQuestionnaire(SHARED_ROOM_ID)
    val content = createChoiceContent(questionnaire)
    val user = authenticate(OTHER_USER_ID)
    answerMutationController.createQuestionnaireChoiceAnswer(
        CreateChoiceAnswerInput(roundIdOf(content), listOf(0)))
    Assertions.assertNotNull(
        answerQueryController.questionnaireAnswerByRoundId(roundIdOf(content), user))
    Assertions.assertNotNull(
        answerQueryController.questionnaireAnswerResultsByQuestionnaireId(
            questionnaire.id!!, user, subrange))
  }

  @Test
  fun shouldWithholdAnswerResultFromParticipantUntilSolutionIsPublished() {
    val questionnaire = createPublishedQuestionnaire(SHARED_ROOM_ID)
    val content = createChoiceContent(questionnaire, listOf(0))
    publishSolution(questionnaire, false)
    val user = authenticate(OTHER_USER_ID)
    val answer =
        answerMutationController.createQuestionnaireChoiceAnswer(
            CreateChoiceAnswerInput(roundIdOf(content), listOf(0)))
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answerQueryController.result(answer))
    val queried = answerQueryController.questionnaireAnswerByRoundId(roundIdOf(content), user)!!
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answerQueryController.result(queried))
    authenticate(OWNER_ID)
    Assertions.assertEquals(AnswerResultState.CORRECT, answerQueryController.result(answer))
    publishSolution(questionnaire, true)
    authenticate(OTHER_USER_ID)
    val published = answerQueryController.questionnaireAnswerByRoundId(roundIdOf(content), user)!!
    Assertions.assertEquals(AnswerResultState.CORRECT, answerQueryController.result(published))
  }

  @Test
  fun shouldWithholdAnswerCountsFromParticipantUntilPublished() {
    val questionnaire = createPublishedQuestionnaire(SHARED_ROOM_ID)
    val content = createChoiceContent(questionnaire, listOf(0))
    publishSolution(questionnaire, false)
    publishStatistics(questionnaire, false)
    authenticate(OTHER_USER_ID)
    answerMutationController.createQuestionnaireChoiceAnswer(
        CreateChoiceAnswerInput(roundIdOf(content), listOf(0)))
    val unpublished = contentQueryController.questionnaireContentById(content.id!!)
    Assertions.assertEquals(listOf(null, null, null), counts(unpublished))
    authenticate(OWNER_ID)
    Assertions.assertEquals(listOf(1, 0, 1), counts(unpublished))
    publishStatistics(questionnaire, true)
    authenticate(OTHER_USER_ID)
    val statisticsPublished = contentQueryController.questionnaireContentById(content.id!!)
    Assertions.assertEquals(listOf(1, 0, null), counts(statisticsPublished))
    publishSolution(questionnaire, true)
    authenticate(OTHER_USER_ID)
    val solutionPublished = contentQueryController.questionnaireContentById(content.id!!)
    Assertions.assertEquals(listOf(1, 0, 1), counts(solutionPublished))
  }

  @Test
  fun shouldLetParticipantSeeStatisticsOnlyOncePublished() {
    val questionnaire = createPublishedQuestionnaire(SHARED_ROOM_ID)
    val content = createChoiceContent(questionnaire)
    questionnaireService.update(questionnaire.id!!, null, null, null, false, null, null)
    authenticate(OTHER_USER_ID)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      roundQueryController.questionnaireChoiceRoundStatsByRoundId(roundIdOf(content))
    }
    authenticate(OWNER_ID)
    questionnaireService.update(questionnaire.id!!, null, null, null, true, null, null)
    authenticate(OTHER_USER_ID)
    Assertions.assertNotNull(
        roundQueryController.questionnaireChoiceRoundStatsByRoundId(roundIdOf(content)))
  }

  @Test
  fun shouldRejectPublishingUpToContentOfOtherQuestionnaire() {
    val otherQuestionnaire = createPublishedQuestionnaire(FOREIGN_ROOM_ID)
    val otherContent = createChoiceContent(otherQuestionnaire)
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      publishingController.publishQuestionnaireUpToContent(
          foreignQuestionnaire.id!!, otherContent.id!!)
    }
  }
}
