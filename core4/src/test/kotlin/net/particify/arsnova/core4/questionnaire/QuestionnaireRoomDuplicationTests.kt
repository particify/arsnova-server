/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireWithNameAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireMutationController
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.ScrollPosition
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

/** Not a member of the rooms [USER_ID] creates here. */
private val OTHER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Room events reach RabbitMQ, which duplication does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireRoomDuplicationTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentRepository: ContentRepository
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireMutationController: QuestionnaireMutationController
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var roundRepository: RoundRepository
  @Autowired lateinit var userService: UserService

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID = USER_ID): User {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
    return user
  }

  private fun createChoiceContent(questionnaireId: UUID, body: String): Content =
      contentService.create(
          questionnaireId,
          body,
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null)

  private fun contentsOf(questionnaire: Questionnaire): List<Content> =
      contentRepository
          .findByQuestionnaireIdOrderByPosition(questionnaire.id!!, ScrollPosition.offset())
          .toList()

  @Test
  fun shouldCopySeriesWithSettingsAndContentsButNoAnswers() {
    val user = authenticate()
    val room =
        roomService.create(Room(shortId = roomService.generateShortId(), name = "Source"), user)
    val survey = questionnaireService.create(room.id!!, "Survey", QuestionnaireType.SURVEY)
    questionnaireService.update(
        survey.id!!, null, true, QuestionnairePublishingMode.UP_TO, false, null, null)
    listOf("First", "Second", "Third").forEach { createChoiceContent(survey.id!!, it) }
    val released = contentsOf(survey)[1]
    publishingService.publishUpToContent(survey.id!!, released.id!!)
    answerService.createAnswer(released.activeRound!!.id!!, ChoiceAnswerFormatData(listOf(0)))
    val quiz = questionnaireService.create(room.id!!, "Quiz", QuestionnaireType.QUIZ)
    createChoiceContent(quiz.id!!, "Only")
    val presented = createChoiceContent(quiz.id!!, "Presented")
    publishingService.startLiveContent(presented.id!!)

    val copyId = roomService.duplicate(room, "Copy", user).id!!

    val copies = questionnaireService.findByRoomIdOrderByName(copyId)
    Assertions.assertEquals(listOf("Quiz", "Survey"), copies.map { it.name })
    val (quizCopy, surveyCopy) = copies
    Assertions.assertEquals(QuestionnaireType.SURVEY, surveyCopy.type)
    Assertions.assertTrue(surveyCopy.published)
    Assertions.assertEquals(QuestionnairePublishingMode.UP_TO, surveyCopy.publishingMode)
    Assertions.assertEquals(false, surveyCopy.statisticsPublished)
    val surveyContents = contentsOf(surveyCopy)
    Assertions.assertEquals(listOf("First", "Second", "Third"), surveyContents.map { it.body })
    // How far the original was released is not copied: every copy starts over.
    Assertions.assertEquals(surveyContents.first().id, surveyCopy.publishedUpToContent?.id)
    Assertions.assertNull(surveyCopy.activeRound)
    surveyContents.forEach { content ->
      val rounds = roundRepository.findByContentIdOrderByRoundNo(content.id!!)
      Assertions.assertEquals(1, rounds.size)
      Assertions.assertTrue(answerRepository.findByRoundId(rounds.single().id!!).isEmpty())
    }
    val quizContents = contentsOf(quizCopy)
    Assertions.assertEquals(listOf("Only", "Presented"), quizContents.map { it.body })
    Assertions.assertEquals(quizContents.first().id, quizCopy.publishedUpToContent?.id)
    Assertions.assertNull(quizCopy.activeRound)
    // A live copy starts over, so its rounds wait to be presented.
    quizContents.forEach { content ->
      Assertions.assertTrue(
          roundRepository.findByContentIdOrderByRoundNo(content.id!!).single().closed)
    }
    Assertions.assertEquals(3, contentsOf(survey).size)
  }

  @Test
  fun shouldDuplicateQuestionnaireWithinItsRoom() {
    val user = authenticate()
    val room =
        roomService.create(Room(shortId = roomService.generateShortId(), name = "Room"), user)
    val original = questionnaireService.create(room.id!!, "Original", QuestionnaireType.MIXED)
    listOf("First", "Second").forEach { createChoiceContent(original.id!!, it) }

    val copy = questionnaireMutationController.duplicateQuestionnaire(original.id!!, "Copy")

    Assertions.assertEquals(room.id, copy.roomId)
    Assertions.assertEquals("Copy", copy.name)
    Assertions.assertEquals(QuestionnaireType.MIXED, copy.type)
    Assertions.assertEquals(listOf("First", "Second"), contentsOf(copy).map { it.body })
    Assertions.assertEquals(
        listOf("Copy", "Original"),
        questionnaireService.findByRoomIdOrderByName(room.id!!).map { it.name })
  }

  @Test
  fun shouldRejectDuplicateWithTakenName() {
    val user = authenticate()
    val room =
        roomService.create(Room(shortId = roomService.generateShortId(), name = "Room"), user)
    val original = questionnaireService.create(room.id!!, "Original", QuestionnaireType.SURVEY)
    questionnaireService.create(room.id!!, "Taken", QuestionnaireType.SURVEY)
    Assertions.assertThrows(QuestionnaireWithNameAlreadyExistsException::class.java) {
      questionnaireMutationController.duplicateQuestionnaire(original.id!!, "Taken")
    }
  }

  @Test
  fun shouldRejectDuplicateByNonMember() {
    val user = authenticate()
    val room =
        roomService.create(Room(shortId = roomService.generateShortId(), name = "Room"), user)
    val original = questionnaireService.create(room.id!!, "Original", QuestionnaireType.SURVEY)
    authenticate(OTHER_USER_ID)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      questionnaireMutationController.duplicateQuestionnaire(original.id!!, "Copy")
    }
  }
}
