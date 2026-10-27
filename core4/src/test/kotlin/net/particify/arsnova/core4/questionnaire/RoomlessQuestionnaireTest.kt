/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.RoomlessQuestionnaireException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.questionnaire.template.RoomlessQuestionnaireChangedEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.AuditorAware
import org.springframework.security.access.PermissionEvaluator

class RoomlessQuestionnaireTest {
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
  private val contentService =
      ContentServiceImpl(
          contentRepository,
          questionnaireRepository,
          eventPublisher,
          Mockito.mock(RoundServiceImpl::class.java),
          answerRepository,
          QuestionnaireEventPublisher())
  private val questionnaireService =
      QuestionnaireServiceImpl(
          questionnaireRepository,
          eventPublisher,
          contentRepository,
          Mockito.mock(RoundServiceImpl::class.java),
          QuestionnaireEventPublisher(),
          contentService)
  private val roomless = Questionnaire(id = UUID.randomUUID(), name = "Template")

  init {
    Mockito.`when`(questionnaireRepository.findById(roomless.id!!))
        .thenReturn(Optional.of(roomless))
    Mockito.`when`(questionnaireRepository.save(roomless)).thenReturn(roomless)
  }

  @Test
  fun shouldTellRoomlessQuestionnairesApart() {
    Assertions.assertTrue(roomless.isRoomless())
    val thrown =
        Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
          roomless.requireRoomId()
        }
    Assertions.assertEquals(roomless.id, thrown.id)
    val roomId = UUID.randomUUID()
    Assertions.assertEquals(roomId, Questionnaire(roomId = roomId).requireRoomId())
  }

  @Test
  fun shouldKeepNameAndPublishingOfATemplatesQuestionnaireToTheTemplate() {
    listOf("Other" to null, null to true).forEach { (name, published) ->
      Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
        questionnaireService.update(roomless.id!!, name, published, null, null, null, null)
      }
    }
    Assertions.assertEquals("Template", roomless.name)
    Assertions.assertFalse(roomless.published)

    questionnaireService.update(
        roomless.id!!, null, null, QuestionnairePublishingMode.UP_TO, true, false, null)
    Assertions.assertEquals(QuestionnairePublishingMode.UP_TO, roomless.publishingMode)
  }

  @Test
  fun shouldAcceptTheCurrentNameAndPublishingOfATemplatesQuestionnaire() {
    questionnaireService.update(roomless.id!!, "Template", false, null, false, null, null)
    Assertions.assertEquals(false, roomless.statisticsPublished)
  }

  @Test
  fun shouldReportChangesToATemplatesQuestionnaire() {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = roomless,
            formatData = WordcloudContentFormatData())
    roomless.contents.add(content)
    Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    Mockito.`when`(contentRepository.save(content)).thenReturn(content)

    questionnaireService.update(roomless.id!!, null, null, null, true, null, null)
    questionnaireService.updateContentOrder(roomless.id!!, listOf(content.id!!))
    contentService.update(content.id!!, "Cities", WordcloudContentFormatData(), null, null, null)
    contentService.delete(content.id!!)

    Mockito.verify(eventPublisher, Mockito.times(4))
        .publishEvent(RoomlessQuestionnaireChangedEvent(roomless.id!!))
  }

  @Test
  fun shouldNotReportChangesToARoomsQuestionnaire() {
    val questionnaire = Questionnaire(id = UUID.randomUUID(), roomId = UUID.randomUUID())
    Mockito.`when`(questionnaireRepository.findById(questionnaire.id!!))
        .thenReturn(Optional.of(questionnaire))
    Mockito.`when`(questionnaireRepository.save(questionnaire)).thenReturn(questionnaire)

    questionnaireService.update(questionnaire.id!!, null, null, null, true, null, null)

    Mockito.verify(eventPublisher, Mockito.never())
        .publishEvent(RoomlessQuestionnaireChangedEvent(questionnaire.id!!))
  }

  @Test
  fun shouldRejectPresentationStateOnATemplatesContents() {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = roomless,
            formatData = WordcloudContentFormatData())
    Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      contentService.banWordcloudKeyword(content.id!!, "spam")
    }
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      contentService.resetBannedWordcloudKeywords(content.id!!)
    }
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldRejectShortAnswerKeywordBansOnATemplatesContents() {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = roomless,
            formatData = ShortAnswerContentFormatData(listOf("Berlin")))
    Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      contentService.banShortAnswerKeyword(content.id!!, "spam")
    }
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      contentService.resetBannedShortAnswerKeywords(content.id!!)
    }
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldRejectAnswersToATemplatesContents() {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = roomless,
            formatData = ChoiceContentFormatData(listOf("A"), null, false))
    val round = Round(id = UUID.randomUUID(), content = content)
    Mockito.`when`(roundRepository.findById(round.id!!)).thenReturn(Optional.of(round))
    val answerService =
        AnswerServiceImpl(
            answerRepository,
            roundRepository,
            contentService,
            RoundStatsEventPublisher(),
            questionnaireRepository,
            AuditorAware { Optional.of(UUID.randomUUID()) },
            Mockito.mock(RoundServiceImpl::class.java),
            Mockito.mock(PermissionEvaluator::class.java))
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    }
    Mockito.verify(answerRepository, Mockito.never()).save(Mockito.any())
  }
}
