/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import reactor.test.StepVerifier

private val SUBSCRIBED_ROUND_ID = UUID.randomUUID()
private val OTHER_ROUND_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val QUESTIONNAIRE_ID = UUID.randomUUID()

// Must match ANSWERS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class AnswerSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val answerServiceImpl = Mockito.mock(AnswerServiceImpl::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val controller =
      AnswerSubscriptionController(eventPublisher, answerServiceImpl, permissionEvaluator)
  private val answers = listOf(Answer(id = UUID.randomUUID()))
  private val subscriberAuthentication = TestingAuthenticationToken("editor", null, "ROLE_USER")

  @BeforeEach
  fun authenticate() {
    SecurityContextHolder.getContext().authentication = subscriberAuthentication
    Mockito.`when`(roundReadPermission()).thenReturn(true)
  }

  private fun roundReadPermission() =
      permissionEvaluator.hasPermission(
          subscriberAuthentication, SUBSCRIBED_ROUND_ID, "Round", "read")

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldEmitAnswersForChangeOfSubscribedRound() {
    Mockito.`when`(answerServiceImpl.findTextAnswersByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(answers)

    StepVerifier.withVirtualTime { controller.questionnaireTextAnswersUpdated(SUBSCRIBED_ROUND_ID) }
        .expectSubscription()
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(answers)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitAnswersForChangeOfOtherRound() {
    StepVerifier.withVirtualTime { controller.questionnaireTextAnswersUpdated(SUBSCRIBED_ROUND_ID) }
        .expectSubscription()
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /**
   * At runtime, answers are fetched on a scheduler thread that never carried the subscriber's
   * security context, so the visibility gating would silently downgrade an editor to a participant
   * unless the controller reinstates it. Virtual time collapses every scheduler onto the verifying
   * thread, so the absent context is reproduced by clearing it instead of by thread separation; the
   * code path being covered — reinstating the snapshot rather than reading the ambient holder — is
   * the same. The stub stands in for the gating in `findTextAnswersByRoundId`.
   */
  @Test
  fun shouldReinstateSubscriberAuthenticationForFetch() {
    val fetchAuthentication = AtomicReference<Authentication>()
    Mockito.`when`(answerServiceImpl.findTextAnswersByRoundId(SUBSCRIBED_ROUND_ID)).thenAnswer {
      val authentication = SecurityContextHolder.getContext().authentication
      fetchAuthentication.set(authentication)
      if (authentication == null) emptyList() else answers
    }
    SecurityContextHolder.getContext().authentication = subscriberAuthentication

    StepVerifier.withVirtualTime { controller.questionnaireTextAnswersUpdated(SUBSCRIBED_ROUND_ID) }
        .expectSubscription()
        // The request that set up the subscription is over by the time answers are fetched.
        .then { SecurityContextHolder.clearContext() }
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(answers)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(subscriberAuthentication, fetchAuthentication.get())
    Assertions.assertNull(SecurityContextHolder.getContext().authentication)
  }

  /** The content can be withdrawn, or the questionnaire unpublished, while subscribed. */
  @Test
  fun shouldStopEmittingAnswersOnceRoundIsNoLongerReadable() {
    Mockito.`when`(answerServiceImpl.findTextAnswersByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(answers)
    Mockito.`when`(roundReadPermission()).thenReturn(true, false)

    StepVerifier.withVirtualTime { controller.questionnaireTextAnswersUpdated(SUBSCRIBED_ROUND_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(answers)
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  private fun publishAnswerChanged(roundId: UUID) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = roundId, contentId = CONTENT_ID, questionnaireId = QUESTIONNAIRE_ID))
  }
}
