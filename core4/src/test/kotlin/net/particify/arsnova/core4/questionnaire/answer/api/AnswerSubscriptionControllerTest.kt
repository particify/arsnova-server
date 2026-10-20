/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.Disposable
import reactor.test.StepVerifier

private val SUBSCRIBED_ROUND_ID = UUID.randomUUID()
private val OTHER_ROUND_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val QUESTIONNAIRE_ID = UUID.randomUUID()

// Must match ANSWERS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

/**
 * Emissions run without the subscriber's security context, so tests clear it once subscribed: the
 * controller has to decide everything subscriber-specific at subscription.
 */
class AnswerSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val answerServiceImpl = Mockito.mock(AnswerServiceImpl::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val controller =
      AnswerSubscriptionController(
          eventPublisher,
          answerServiceImpl,
          roundRepository,
          ContentPermissionEvaluator(Mockito.mock(ContentRepository::class.java)),
          permissionEvaluator)
  private val participant = TestingAuthenticationToken("participant", null, "ROLE_USER")
  private val editor = TestingAuthenticationToken("editor", null, "ROLE_USER")
  private val questionnaire = Questionnaire(id = QUESTIONNAIRE_ID, published = true)
  private val content =
      Content(
          id = CONTENT_ID,
          questionnaire = questionnaire,
          position = 0,
          formatData = TextContentFormatData(answersPublished = true))
  private val visibleAnswer = Answer(id = UUID.randomUUID())
  private val hiddenAnswer = Answer(id = UUID.randomUUID(), hidden = true)
  private val allAnswers = listOf(visibleAnswer, hiddenAnswer)
  private val participantAnswers = listOf(visibleAnswer)

  @BeforeEach
  fun setUp() {
    Mockito.`when`(roundRepository.findById(SUBSCRIBED_ROUND_ID))
        .thenReturn(Optional.of(Round(id = SUBSCRIBED_ROUND_ID, content = content)))
    Mockito.`when`(answerServiceImpl.findByRoundIdOrderByIdAsc(SUBSCRIBED_ROUND_ID))
        .thenReturn(allAnswers)
    Mockito.`when`(permissionEvaluator.hasPermission(editor, CONTENT_ID, "Content", "write"))
        .thenReturn(true)
    authenticate(participant)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(authentication: Authentication) {
    SecurityContextHolder.getContext().authentication = authentication
  }

  private fun subscribe() = controller.questionnaireTextAnswersUpdated(SUBSCRIBED_ROUND_ID)

  @Test
  fun shouldEmitAnswersForChangeOfSubscribedRound() {
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(participantAnswers)
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(participantAnswers)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitAnswersForChangeOfOtherRound() {
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(participantAnswers)
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** Editors need the hidden answers to be able to unhide them. */
  @Test
  fun shouldEmitHiddenAnswersToEditorOnly() {
    authenticate(editor)
    val participantEmissions = mutableListOf<List<Answer>>()
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(allAnswers)
        .then {
          authenticate(participant)
          subscribe().subscribe { participantEmissions.add(it) }.dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(participantAnswers), participantEmissions)
  }

  @Test
  fun shouldEmitNoAnswersToParticipantBeforeTheyArePublished() {
    content.formatData = TextContentFormatData(answersPublished = false)

    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(emptyList())
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /**
   * The content can be withdrawn, or the questionnaire unpublished, while subscribed. The
   * permission is decided at subscription only, never for an emission.
   */
  @Test
  fun shouldStopEmittingAnswersOnceContentIsNoLongerReleased() {
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(participantAnswers)
        .then {
          SecurityContextHolder.clearContext()
          questionnaire.published = false
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(permissionEvaluator).hasPermission(participant, CONTENT_ID, "Content", "write")
    Mockito.verifyNoMoreInteractions(permissionEvaluator)
  }

  @Test
  fun shouldShareOneComputationAmongSubscribers() {
    val second = mutableListOf<List<Answer>>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(participantAnswers)
        .then {
          authenticate(editor)
          secondSubscription = subscribe().subscribe { second.add(it) }
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(participantAnswers)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(allAnswers, allAnswers), second)
    Mockito.verify(answerServiceImpl, Mockito.times(2))
        .findByRoundIdOrderByIdAsc(SUBSCRIBED_ROUND_ID)
  }

  /** The answers replayed to a late joiner are shared, so they have to pass the joiner's gate. */
  @Test
  fun shouldHandALateSubscriberTheLastAnswersOnlyIfPermitted() {
    questionnaire.published = false
    authenticate(editor)
    val late = mutableListOf<List<Answer>>()
    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(allAnswers)
        .then {
          authenticate(participant)
          subscribe().subscribe { late.add(it) }.dispose()
          Assertions.assertEquals(emptyList<List<Answer>>(), late)
          questionnaire.published = true
          subscribe().subscribe { late.add(it) }.dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(participantAnswers), late)
    Mockito.verify(answerServiceImpl, Mockito.times(1))
        .findByRoundIdOrderByIdAsc(SUBSCRIBED_ROUND_ID)
  }

  @Test
  fun shouldSkipAFailedRecomputationAndKeepEmitting() {
    Mockito.`when`(answerServiceImpl.findByRoundIdOrderByIdAsc(SUBSCRIBED_ROUND_ID))
        .thenReturn(allAnswers)
        .thenThrow(TransientDataAccessResourceException("Connection lost"))
        .thenReturn(allAnswers)

    StepVerifier.withVirtualTime { subscribe() }
        .expectSubscription()
        .expectNext(participantAnswers)
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNoEvent(SAMPLE_INTERVAL)
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(participantAnswers)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  private fun publishAnswerChanged(roundId: UUID) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = roundId, contentId = CONTENT_ID, questionnaireId = QUESTIONNAIRE_ID))
  }
}
