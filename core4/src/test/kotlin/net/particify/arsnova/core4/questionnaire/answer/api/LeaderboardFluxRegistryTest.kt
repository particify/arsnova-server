/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.RawLeaderboardEntry
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.room.RoomUserAliasProjection
import net.particify.arsnova.core4.room.event.RoomUserAliasChangedEvent
import org.jmolecules.event.annotation.DomainEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.transaction.event.TransactionalEventListener
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val OTHER_QUESTIONNAIRE_ID = UUID.randomUUID()
private val ROOM_ID = UUID.randomUUID()
private val OTHER_ROOM_ID = UUID.randomUUID()
private val USER_ID = UUID.randomUUID()

// Must match LEADERBOARD_UPDATE_SAMPLE_INTERVAL of the registry under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class LeaderboardFluxRegistryTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val questionnaireEventPublisher = QuestionnaireEventPublisher()
  private val leaderboardService = Mockito.mock(LeaderboardServiceImpl::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val registry =
      LeaderboardFluxRegistry(
          eventPublisher, questionnaireEventPublisher, leaderboardService, questionnaireRepository)
  private val ranking = listOf(RawLeaderboardEntry(USER_ID, null, 500, 1, null))
  private val renamed =
      listOf(ranking.single().copy(alias = RoomUserAliasProjection(USER_ID, null, "Beta", null)))

  init {
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID))
        .thenReturn(Optional.of(Questionnaire(id = QUESTIONNAIRE_ID, roomId = ROOM_ID)))
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

  private fun publishAliasChanged(roomId: UUID, userId: UUID) {
    registry.onRoomUserAliasChanged(RoomUserAliasChangedEvent(roomId, userId))
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
   * A participant who sets an alias after answering would otherwise stay anonymous in the ranking
   * replayed to everyone joining later, until the next answer. That holds before any answer arrived
   * while the stream was open as well.
   */
  @Test
  fun shouldRecomputeRightAwayWhenARankedUserSetsAnAlias() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then {
          Mockito.`when`(
                  leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null))
              .thenReturn(renamed)
          publishAliasChanged(ROOM_ID, USER_ID)
        }
        .expectNext(renamed)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
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
  fun shouldIgnoreAliasesOfUsersWhoAreNotRanked() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then { publishAliasChanged(ROOM_ID, UUID.randomUUID()) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(leaderboardService, Mockito.times(1))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  /** An alias belongs to a single room, so a ranked user renaming themselves elsewhere is moot. */
  @Test
  fun shouldIgnoreAliasesSetInOtherRooms() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .expectNext(ranking)
        .then { publishAliasChanged(OTHER_ROOM_ID, USER_ID) }
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

  /** The registry must only see the alias after its commit, and as a declared domain event. */
  @Test
  fun shouldReceiveAliasChangesAsDomainEventsAfterTheCommit() {
    Assertions.assertTrue(
        RoomUserAliasChangedEvent::class.java.isAnnotationPresent(DomainEvent::class.java))
    val listener =
        LeaderboardFluxRegistry::class
            .java
            .getMethod("onRoomUserAliasChanged", RoomUserAliasChangedEvent::class.java)
    Assertions.assertNotNull(listener.getAnnotation(TransactionalEventListener::class.java))
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
              .thenReturn(renamed)
          publishAnswerChanged(QUESTIONNAIRE_ID)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNoEvent(SAMPLE_INTERVAL)
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(renamed)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(ranking, renamed), second)
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

  /** A failure to set the stream up is passed on, but the next subscriber starts over. */
  @Test
  fun shouldNotHandOnAStreamThatFailedToStart() {
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID))
        .thenThrow(TransientDataAccessResourceException("Connection lost"))
        .thenReturn(Optional.of(Questionnaire(id = QUESTIONNAIRE_ID, roomId = ROOM_ID)))
    val failed = registry.leaderboardFlux(QUESTIONNAIRE_ID, null)
    // A subscriber reacting to the error right away must not be handed the failed stream either.
    val handedOnError = AtomicReference<Flux<List<RawLeaderboardEntry>>>()
    StepVerifier.create(
            failed.doOnError {
              handedOnError.set(registry.leaderboardFlux(QUESTIONNAIRE_ID, null))
            })
        .expectError(TransientDataAccessResourceException::class.java)
        .verify(VERIFY_TIMEOUT)
    Assertions.assertNotSame(failed, handedOnError.get())

    val retried = registry.leaderboardFlux(QUESTIONNAIRE_ID, null)
    Assertions.assertNotSame(failed, retried)
    StepVerifier.create(retried).expectNext(ranking).thenCancel().verify(VERIFY_TIMEOUT)
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
