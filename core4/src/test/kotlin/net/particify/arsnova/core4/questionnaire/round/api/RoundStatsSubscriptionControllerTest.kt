/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.round.ChoiceContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.NumericContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundStatsServiceImpl
import net.particify.arsnova.core4.questionnaire.round.WordcloudContentRoundStats
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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

// Must match STATS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

/**
 * Emissions run without the subscriber's security context, so tests clear it once subscribed: the
 * controller has to decide everything subscriber-specific at subscription.
 */
class RoundStatsSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val roundStatsServiceImpl = Mockito.mock(RoundStatsServiceImpl::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val participant = TestingAuthenticationToken("participant", null, "ROLE_USER")
  private val moderator = TestingAuthenticationToken("moderator", null, "ROLE_USER")
  private val controller =
      RoundStatsSubscriptionController(
          eventPublisher,
          roundStatsServiceImpl,
          roundRepository,
          ContentPermissionEvaluator(Mockito.mock(ContentRepository::class.java)),
          permissionEvaluator)
  private val questionnaire =
      Questionnaire(id = QUESTIONNAIRE_ID, published = true, statisticsPublished = true)
  private val content = Content(id = CONTENT_ID, questionnaire = questionnaire, position = 0)
  private val choiceStats = ChoiceContentRoundStats(listOf(2, 1))

  @BeforeEach
  fun setUp() {
    Mockito.`when`(roundRepository.findById(SUBSCRIBED_ROUND_ID))
        .thenReturn(Optional.of(Round(id = SUBSCRIBED_ROUND_ID, content = content)))
    Mockito.`when`(roundStatsServiceImpl.getChoiceRoundStatsByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(choiceStats)
    Mockito.`when`(permissionEvaluator.hasPermission(moderator, questionnaire, "moderate"))
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

  private fun subscribeChoice() =
      controller.questionnaireChoiceRoundStatsUpdated(SUBSCRIBED_ROUND_ID)

  @Test
  fun shouldStartWithTheCurrentStats() {
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEmitChoiceStatsForAnswerOfSubscribedRound() {
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(choiceStats)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitChoiceStatsForAnswerOfOtherRound() {
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /**
   * The solution is computed once for all subscribers and left out for each who may not see it,
   * including the stats replayed to one joining later.
   */
  @Test
  fun shouldLeaveTheSolutionOutOfNumericStatsForParticipantOnly() {
    val stats = givenNumericStats()
    authenticate(moderator)
    val participantEmissions = mutableListOf<NumericContentRoundStats>()
    StepVerifier.withVirtualTime {
          controller.questionnaireNumericRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .expectNext(stats)
        .then {
          authenticate(participant)
          controller
              .questionnaireNumericRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
              .subscribe { participantEmissions.add(it) }
              .dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(stats.withoutSolution()), participantEmissions)
    Mockito.verify(roundStatsServiceImpl, Mockito.times(1))
        .getNumericRoundStatsWithSolutionByRoundId(SUBSCRIBED_ROUND_ID)
  }

  @Test
  fun shouldIncludeTheSolutionInNumericStatsOncePublished() {
    val stats = givenNumericStats()

    StepVerifier.withVirtualTime {
          controller.questionnaireNumericRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .expectNext(stats.withoutSolution())
        .then {
          SecurityContextHolder.clearContext()
          questionnaire.correctOptionsPublished = true
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(stats)
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEmitWordcloudStatsForAnswerOfSubscribedRound() {
    val stats = WordcloudContentRoundStats(listOf("word"), listOf(1))
    Mockito.`when`(roundStatsServiceImpl.getWordcloudRoundStatsByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(stats)

    StepVerifier.withVirtualTime {
          controller.questionnaireWordcloudRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .expectNext(stats)
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(stats)
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldShareOneComputationAmongSubscribers() {
    val second = mutableListOf<ChoiceContentRoundStats>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then {
          secondSubscription = subscribeChoice().subscribe { second.add(it) }
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(choiceStats)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(choiceStats, choiceStats), second)
    Mockito.verify(roundStatsServiceImpl, Mockito.times(2))
        .getChoiceRoundStatsByRoundId(SUBSCRIBED_ROUND_ID)
  }

  /** The permission is decided at subscription only, never for an emission. */
  @Test
  fun shouldStopEmittingStatsOnceUnpublished() {
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then {
          SecurityContextHolder.clearContext()
          questionnaire.statisticsPublished = false
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(permissionEvaluator).hasPermission(participant, questionnaire, "moderate")
    Mockito.verifyNoMoreInteractions(permissionEvaluator)
  }

  @Test
  fun shouldStopEmittingStatsOnceContentIsNoLongerReleased() {
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then {
          questionnaire.published = false
          publishAnswerChanged(SUBSCRIBED_ROUND_ID)
        }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEmitUnpublishedStatsToModerator() {
    questionnaire.statisticsPublished = false
    authenticate(moderator)

    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** The stats replayed to a late joiner are shared, so they have to pass the joiner's gate. */
  @Test
  fun shouldHandALateSubscriberTheLastStatsOnlyIfPermitted() {
    questionnaire.statisticsPublished = false
    authenticate(moderator)
    val late = mutableListOf<ChoiceContentRoundStats>()
    StepVerifier.withVirtualTime { subscribeChoice() }
        .expectSubscription()
        .expectNext(choiceStats)
        .then {
          authenticate(participant)
          subscribeChoice().subscribe { late.add(it) }.dispose()
          Assertions.assertEquals(emptyList<ChoiceContentRoundStats>(), late)
          questionnaire.statisticsPublished = true
          subscribeChoice().subscribe { late.add(it) }.dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(choiceStats), late)
    Mockito.verify(roundStatsServiceImpl, Mockito.times(1))
        .getChoiceRoundStatsByRoundId(SUBSCRIBED_ROUND_ID)
  }

  private fun givenNumericStats(): NumericContentRoundStats {
    questionnaire.correctOptionsPublished = false
    val stats =
        NumericContentRoundStats(
            selectedNumbers = listOf(1.0, 2.0),
            counts = listOf(1, 2),
            mean = 1.5,
            median = 1.5,
            standardDeviation = 0.5,
            variance = 0.25,
            minimum = 1.0,
            maximum = 2.0,
            correctAnswerFraction = 0.5)
    Mockito.`when`(
            roundStatsServiceImpl.getNumericRoundStatsWithSolutionByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(stats)
    return stats
  }

  private fun publishAnswerChanged(roundId: UUID) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = roundId, contentId = CONTENT_ID, questionnaireId = QUESTIONNAIRE_ID))
  }
}
