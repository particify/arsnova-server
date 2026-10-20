/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
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
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ContentMoveTests {
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireEventPublisher: QuestionnaireEventPublisher
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

  private fun createQuestionnaire(type: QuestionnaireType): Questionnaire =
      questionnaireService.create(ROOM_ID, "Move ${UUID.randomUUID()}", type)

  private fun createContent(questionnaire: Questionnaire, body: String): Content =
      contentService.create(
          questionnaire.id!!,
          body,
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          DURATION,
          true,
          null)

  private fun reload(questionnaire: Questionnaire): Questionnaire =
      questionnaireService.findByIdOrNull(questionnaire.id!!)!!

  private fun bodiesOf(questionnaire: Questionnaire): List<String?> =
      contentService
          .findByQuestionnaireIdOrderByPosition(questionnaire.id!!, ScrollPosition.offset())
          .toList()
          .map { it.body }

  private fun roundOf(content: Content): Round =
      roundService.findByContentIdOrderByRoundNo(content.id!!).single()

  @Test
  fun shouldMoveContentToTheEndOfTheTarget() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val moved = createContent(source, "Moved")
    createContent(source, "Stays")
    val target = createQuestionnaire(QuestionnaireType.SURVEY)
    createContent(target, "Existing")
    contentService.moveContentToQuestionnaire(moved.id!!, target.id!!)
    Assertions.assertEquals(listOf("Stays"), bodiesOf(source))
    Assertions.assertEquals(listOf("Existing", "Moved"), bodiesOf(target))
    Assertions.assertEquals(
        target.id, contentService.findByIdOrNull(moved.id!!)!!.questionnaire?.id)
  }

  @Test
  fun shouldShrinkThePublishedRangeOfTheSourceWhenMovingItsLastReleasedContent() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val first = createContent(source, "First")
    val second = createContent(source, "Second")
    createContent(source, "Third")
    questionnaireService.update(
        source.id!!, null, null, QuestionnairePublishingMode.UP_TO, null, null, null)
    publishingService.publishUpToContent(source.id!!, second.id!!)
    val target = createQuestionnaire(QuestionnaireType.SURVEY)
    contentService.moveContentToQuestionnaire(second.id!!, target.id!!)
    Assertions.assertEquals(first.id, reload(source).publishedUpToContent?.id)
  }

  @Test
  fun shouldReleaseTheActiveRoundOfTheSourceWhenMovingThePresentedContent() {
    val source = createQuestionnaire(QuestionnaireType.QUIZ)
    val presented = createContent(source, "Presented")
    publishingService.startLiveContent(presented.id!!)
    val target = createQuestionnaire(QuestionnaireType.SURVEY)
    contentService.moveContentToQuestionnaire(presented.id!!, target.id!!)
    val reloadedSource = reload(source)
    Assertions.assertNull(reloadedSource.activeRound)
    Assertions.assertNull(reloadedSource.publishedUpToContent)
    val round = roundOf(presented)
    Assertions.assertFalse(round.closed)
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertNull(round.answeringPausedAt)
  }

  @Test
  fun shouldCloseTheRoundWhenMovingIntoLiveMode() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val moved = createContent(source, "Moved")
    Assertions.assertFalse(roundOf(moved).closed)
    val target = createQuestionnaire(QuestionnaireType.QUIZ)
    contentService.moveContentToQuestionnaire(moved.id!!, target.id!!)
    val round = roundOf(moved)
    Assertions.assertTrue(round.closed)
    Assertions.assertNull(round.answeringEndTime)
  }

  @Test
  fun shouldReleaseTheMovedContentWhenTheTargetHasNothingReleased() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val moved = createContent(source, "Moved")
    val target = createQuestionnaire(QuestionnaireType.QUIZ)
    Assertions.assertNull(reload(target).publishedUpToContent)
    contentService.moveContentToQuestionnaire(moved.id!!, target.id!!)
    Assertions.assertEquals(moved.id, reload(target).publishedUpToContent?.id)
  }

  @Test
  fun shouldRejectMoveOfUnknownContentOrIntoUnknownQuestionnaire() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createContent(source, "Content")
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      contentService.moveContentToQuestionnaire(UUID.randomUUID(), source.id!!)
    }
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      contentService.moveContentToQuestionnaire(content.id!!, UUID.randomUUID())
    }
  }

  @Test
  fun shouldSignalBothQuestionnaires() {
    val source = createQuestionnaire(QuestionnaireType.SURVEY)
    val moved = createContent(source, "Moved")
    val target = createQuestionnaire(QuestionnaireType.SURVEY)
    val updated = mutableListOf<UUID>()
    val subscription =
        questionnaireEventPublisher.questionnaireUpdatedFlux().subscribe { updated.add(it) }
    try {
      contentService.moveContentToQuestionnaire(moved.id!!, target.id!!)
    } finally {
      subscription.dispose()
    }
    Assertions.assertTrue(updated.containsAll(listOf(source.id!!, target.id!!)))
  }
}
