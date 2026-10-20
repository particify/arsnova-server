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
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.ScrollPosition
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

/** Room events reach RabbitMQ, which the cascade does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireRoomDeletionTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentRepository: ContentRepository
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireRepository: QuestionnaireRepository
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var roundRepository: RoundRepository
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var userService: UserService

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createRoom(): Room {
    val user = userService.loadUserById(USER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
    return roomService.create(
        Room(shortId = roomService.generateShortId(), name = "Deletion ${UUID.randomUUID()}"), user)
  }

  private fun createChoiceContent(questionnaireId: UUID): Content =
      contentService.create(
          questionnaireId,
          "Choice",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null)

  @Test
  fun shouldDeleteQuestionnairesWithContentsRoundsAndAnswersWithRoom() {
    val roomId = createRoom().id!!
    val quiz = questionnaireService.create(roomId, "Quiz", QuestionnaireType.QUIZ)
    val survey = questionnaireService.create(roomId, "Survey", QuestionnaireType.SURVEY)
    val presented = createChoiceContent(quiz.id!!)
    createChoiceContent(quiz.id!!)
    val surveyContent = createChoiceContent(survey.id!!)
    // Leaves the quiz with an active round and a published range pointing into its contents.
    val liveRound = publishingService.startLiveContent(presented.id!!).activeRound!!
    answerService.createAnswer(liveRound.id!!, ChoiceAnswerFormatData(listOf(0)))
    val secondRound = roundService.startNewRound(surveyContent.id!!)
    answerService.createAnswer(secondRound.id!!, ChoiceAnswerFormatData(listOf(1)))
    val contentIds =
        listOf(quiz, survey).flatMap { questionnaire ->
          contentRepository
              .findByQuestionnaireIdOrderByPosition(questionnaire.id!!, ScrollPosition.offset())
              .map { it.id!! }
        }
    val roundIds =
        contentIds.flatMap { roundRepository.findByContentIdOrderByRoundNo(it) }.map { it.id!! }

    roomService.deleteById(roomId)

    Assertions.assertTrue(questionnaireService.findByRoomIdOrderByName(roomId).isEmpty())
    Assertions.assertEquals(3, contentIds.size)
    Assertions.assertTrue(contentRepository.findAllById(contentIds).isEmpty())
    Assertions.assertTrue(roundRepository.findAllById(roundIds).isEmpty())
    roundIds.forEach { Assertions.assertTrue(answerRepository.findByRoundId(it).isEmpty()) }
    Assertions.assertFalse(questionnaireRepository.existsById(quiz.id!!))
    Assertions.assertFalse(roomService.existsById(roomId))
  }
}
