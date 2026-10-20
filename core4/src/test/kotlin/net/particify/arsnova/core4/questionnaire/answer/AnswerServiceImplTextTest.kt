/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.AnswerNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.AuditorAware
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import reactor.test.StepVerifier

private val ROUND_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val VERIFY_TIMEOUT = Duration.ofSeconds(5)

class AnswerServiceImplTextTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val contentServiceImpl = Mockito.mock(ContentServiceImpl::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val eventPublisher = RoundStatsEventPublisher()
  private val answerService =
      AnswerServiceImpl(
          answerRepository,
          roundRepository,
          contentServiceImpl,
          eventPublisher,
          Mockito.mock(QuestionnaireRepository::class.java),
          AuditorAware { Optional.of(UUID.randomUUID()) },
          Mockito.mock(RoundServiceImpl::class.java),
          permissionEvaluator)
  private val visibleAnswers = listOf(Answer(id = UUID.randomUUID()))
  private val allAnswers = visibleAnswers + Answer(id = UUID.randomUUID(), hidden = true)
  private val roomId = UUID.randomUUID()
  private val authentication = TestingAuthenticationToken("user", null, "ROLE_USER")

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun givenRound(formatData: ContentFormatData): Round {
    val content =
        Content(
            id = CONTENT_ID,
            questionnaire = Questionnaire(id = UUID.randomUUID(), roomId = roomId),
            formatData = formatData)
    val round = Round(id = ROUND_ID, content = content)
    Mockito.`when`(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round))
    Mockito.`when`(contentServiceImpl.asTextFormatData(content)).thenCallRealMethod()
    Mockito.`when`(answerRepository.findByRoundIdAndHiddenFalseOrderByIdAsc(ROUND_ID))
        .thenReturn(visibleAnswers)
    Mockito.`when`(answerRepository.findByRoundIdOrderByIdAsc(ROUND_ID)).thenReturn(allAnswers)
    return round
  }

  private fun givenPermissions(mayEdit: Boolean, mayModerate: Boolean = false) {
    SecurityContextHolder.getContext().authentication = authentication
    Mockito.`when`(
            permissionEvaluator.hasPermission(authentication, CONTENT_ID, "Content", "write"))
        .thenReturn(mayEdit)
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, roomId, "Room", "moderate"))
        .thenReturn(mayModerate)
  }

  @Test
  fun shouldShowPublishedUnhiddenAnswersToParticipants() {
    givenRound(TextContentFormatData(true))
    givenPermissions(mayEdit = false)
    Assertions.assertEquals(visibleAnswers, answerService.findTextAnswersByRoundId(ROUND_ID))
  }

  @Test
  fun shouldShowPublishedUnhiddenAnswersWithoutAuthentication() {
    givenRound(TextContentFormatData(true))
    Assertions.assertEquals(visibleAnswers, answerService.findTextAnswersByRoundId(ROUND_ID))
    Mockito.verifyNoInteractions(permissionEvaluator)
  }

  @Test
  fun shouldShowEveryAnswerToEditorsAndModerators() {
    listOf(true, false).forEach { published ->
      givenRound(TextContentFormatData(published))
      givenPermissions(mayEdit = true)
      Assertions.assertEquals(
          allAnswers, answerService.findTextAnswersByRoundId(ROUND_ID), "editor $published")
      givenPermissions(mayEdit = false, mayModerate = true)
      Assertions.assertEquals(
          allAnswers, answerService.findTextAnswersByRoundId(ROUND_ID), "moderator $published")
    }
  }

  @Test
  fun shouldWithholdUnpublishedAnswersFromParticipants() {
    givenRound(TextContentFormatData(false))
    givenPermissions(mayEdit = false)
    Assertions.assertEquals(listOf<Answer>(), answerService.findTextAnswersByRoundId(ROUND_ID))
    Mockito.verify(answerRepository, Mockito.never())
        .findByRoundIdAndHiddenFalseOrderByIdAsc(ROUND_ID)
    Mockito.verify(answerRepository, Mockito.never()).findByRoundIdOrderByIdAsc(ROUND_ID)
  }

  @Test
  fun shouldWithholdUnpublishedAnswersWithoutAuthentication() {
    givenRound(TextContentFormatData(false))
    Assertions.assertEquals(listOf<Answer>(), answerService.findTextAnswersByRoundId(ROUND_ID))
    Mockito.verifyNoInteractions(permissionEvaluator)
  }

  @Test
  fun shouldRejectTextAnswerQueryForUnknownRoundOrOtherFormat() {
    Assertions.assertThrows(RoundNotFoundException::class.java) {
      answerService.findTextAnswersByRoundId(ROUND_ID)
    }
    givenRound(ChoiceContentFormatData(listOf("A"), null, false))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      answerService.findTextAnswersByRoundId(ROUND_ID)
    }
  }

  @Test
  fun shouldHideAndShowAnswerAndNotifyItsRound() {
    val answer =
        Answer(
            id = UUID.randomUUID(),
            round = givenRound(TextContentFormatData(true)),
            formatData = TextAnswerFormatData("An answer"))
    Mockito.`when`(answerRepository.findById(answer.id!!)).thenReturn(Optional.of(answer))
    Mockito.`when`(answerRepository.save(answer)).thenReturn(answer)

    StepVerifier.create(eventPublisher.answerChangedFlux().take(2))
        .then { Assertions.assertTrue(answerService.updateHidden(answer.id!!, true).hidden) }
        .assertNext { Assertions.assertEquals(ROUND_ID, it.roundId) }
        .then { Assertions.assertFalse(answerService.updateHidden(answer.id!!, false).hidden) }
        .assertNext { Assertions.assertEquals(CONTENT_ID, it.contentId) }
        .expectComplete()
        .verify(VERIFY_TIMEOUT)
    Mockito.verify(answerRepository, Mockito.times(2)).save(answer)
  }

  @Test
  fun shouldHideWordcloudAnswer() {
    val answer =
        Answer(
            id = UUID.randomUUID(),
            round = givenRound(WordcloudContentFormatData()),
            formatData = WordcloudAnswerFormatData(listOf("spam")))
    Mockito.`when`(answerRepository.findById(answer.id!!)).thenReturn(Optional.of(answer))
    Mockito.`when`(answerRepository.save(answer)).thenReturn(answer)
    Assertions.assertTrue(answerService.updateHidden(answer.id!!, true).hidden)
  }

  @Test
  fun shouldHideShortAnswer() {
    val answer =
        Answer(
            id = UUID.randomUUID(),
            round = givenRound(ShortAnswerContentFormatData(listOf("Offenbach"))),
            formatData = ShortAnswerAnswerFormatData("Hanau"))
    Mockito.`when`(answerRepository.findById(answer.id!!)).thenReturn(Optional.of(answer))
    Mockito.`when`(answerRepository.save(answer)).thenReturn(answer)
    Assertions.assertTrue(answerService.updateHidden(answer.id!!, true).hidden)
  }

  @Test
  fun shouldRejectHidingChosenAnswerOrAbstention() {
    val round = givenRound(ChoiceContentFormatData(listOf("A"), null, false))
    listOf(ChoiceAnswerFormatData(listOf(0)), null).forEach {
      val answer = Answer(id = UUID.randomUUID(), round = round, formatData = it)
      Mockito.`when`(answerRepository.findById(answer.id!!)).thenReturn(Optional.of(answer))
      Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
        answerService.updateHidden(answer.id!!, true)
      }
      Assertions.assertFalse(answer.hidden)
    }
    Mockito.verify(answerRepository, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldRejectHidingUnknownAnswer() {
    Assertions.assertThrows(AnswerNotFoundException::class.java) {
      answerService.updateHidden(UUID.randomUUID(), true)
    }
  }
}
