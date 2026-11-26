/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.RawLeaderboardEntry
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.TransientDataAccessResourceException
import reactor.core.Disposable
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val OTHER_QUESTIONNAIRE_ID = UUID.randomUUID()
private val USER_ID = UUID.randomUUID()

// Must match LEADERBOARD_UPDATE_SAMPLE_INTERVAL of the registry under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class LeaderboardFluxRegistryTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val questionnaireEventPublisher = QuestionnaireEventPublisher()
  private val leaderboardService = Mockito.mock(LeaderboardServiceImpl::class.java)
  private val registry =
      LeaderboardFluxRegistry(eventPublisher, questionnaireEventPublisher, leaderboardService)
  private val ranking = listOf(RawLeaderboardEntry(USER_ID, null, 500, 1, null))
  private val updated = listOf(ranking.single().copy(score = 1000))

  init {
    Mockito.`when`(leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null))
        .thenReturn(ranking)
  }

  private fun publishAnswerChanged(questionnaireId: UUID) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = UUID.randomUUID(),
            contentId = UUID.randomUUID(),
            questionnaireId = questionnaireId))
  }

  @Test
  fun shouldStartWithTheCurrentRanking() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldShareOneComputationAmongSubscribers() {
    val second = mutableListOf<List<RawLeaderboardEntry>>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then {
          secondSubscription =
              registry.leaderboardFlux(QUESTIONNAIRE_ID, null).subscribe { second.add(it) }
          publishAnswerChanged(QUESTIONNAIRE_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(ranking)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(ranking, ranking), second)
    Mockito.verify(leaderboardService, Mockito.times(2))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  @Test
  fun shouldHandALateSubscriberTheLastRankingWithoutRecomputing() {
    val late = mutableListOf<List<RawLeaderboardEntry>>()
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then {
          registry.leaderboardFlux(QUESTIONNAIRE_ID, null).subscribe { late.add(it) }.dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(ranking), late)
    Mockito.verify(leaderboardService, Mockito.times(1))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  /**
   * Correctness and points are withheld until the solution is published, and the next answer that
   * would otherwise recompute the ranking may never come.
   */
  @Test
  fun shouldRecomputeWhenTheSolutionIsPublished() {
    val published = listOf(ranking.single().copy(solutionPublished = true))
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then {
          Mockito.`when`(
                  leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null))
              .thenReturn(published)
          questionnaireEventPublisher.publishQuestionnaireUpdated(QUESTIONNAIRE_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(published)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldIgnoreUpdatesOfOtherQuestionnaires() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then { questionnaireEventPublisher.publishQuestionnaireUpdated(OTHER_QUESTIONNAIRE_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(leaderboardService, Mockito.times(1))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  @Test
  fun shouldIgnoreAnswersToOtherQuestionnaires() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then { publishAnswerChanged(OTHER_QUESTIONNAIRE_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(leaderboardService, Mockito.times(1))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  /** A failure ending the shared stream would end it for every subscriber of the leaderboard. */
  @Test
  fun shouldSkipAFailedComputationAndKeepServingEverySubscriber() {
    val second = mutableListOf<List<RawLeaderboardEntry>>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then {
          secondSubscription =
              registry.leaderboardFlux(QUESTIONNAIRE_ID, null).subscribe { second.add(it) }
          Mockito.`when`(
                  leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null))
              .thenThrow(TransientDataAccessResourceException("Connection lost"))
              .thenReturn(updated)
          publishAnswerChanged(QUESTIONNAIRE_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNoEvent(SAMPLE_INTERVAL)
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(updated)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(ranking, updated), second)
  }

  @Test
  fun shouldKeepTheStreamAliveWhenTheContentIsDeletedMeanwhile() {
    val contentId = UUID.randomUUID()
    Mockito.`when`(leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, contentId))
        .thenReturn(ranking)
        .thenThrow(ContentNotFoundException(contentId))
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, contentId) }
        .expectSubscription()
        .expectNext(ranking)
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNoEvent(SAMPLE_INTERVAL)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldKeepSeparateComputationsPerContent() {
    val contentId = UUID.randomUUID()
    Assertions.assertNotSame(
        registry.leaderboardFlux(QUESTIONNAIRE_ID, null),
        registry.leaderboardFlux(QUESTIONNAIRE_ID, contentId))
    Assertions.assertSame(
        registry.leaderboardFlux(QUESTIONNAIRE_ID, contentId),
        registry.leaderboardFlux(QUESTIONNAIRE_ID, contentId))
  }

  @Test
  fun shouldUnregisterOnceTheLastSubscriberHasLeft() {
    val shared = registry.leaderboardFlux(QUESTIONNAIRE_ID, null)
    val subscription = shared.subscribe()
    Assertions.assertSame(shared, registry.leaderboardFlux(QUESTIONNAIRE_ID, null))

    subscription.dispose()

    Assertions.assertNotSame(shared, registry.leaderboardFlux(QUESTIONNAIRE_ID, null))
  }
}
