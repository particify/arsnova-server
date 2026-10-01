/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.RawLeaderboardEntry
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.room.RoomUserAliasChangedEvent
import net.particify.arsnova.core4.room.RoomUserAliasEventPublisher
import net.particify.arsnova.core4.room.RoomUserAliasProjection
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.core.Disposable
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val OTHER_QUESTIONNAIRE_ID = UUID.randomUUID()
private val ROOM_ID = UUID.randomUUID()
private val USER_ID = UUID.randomUUID()

// Must match LEADERBOARD_UPDATE_SAMPLE_INTERVAL of the registry under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class LeaderboardFluxRegistryTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val aliasEventPublisher = RoomUserAliasEventPublisher()
  private val leaderboardService = Mockito.mock(LeaderboardServiceImpl::class.java)
  private val registry =
      LeaderboardFluxRegistry(eventPublisher, aliasEventPublisher, leaderboardService)
  private val ranking = listOf(RawLeaderboardEntry(USER_ID, null, 500, 1, null))

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
  fun shouldShareOneComputationAmongSubscribers() {
    val second = mutableListOf<List<RawLeaderboardEntry>>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
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

    Assertions.assertEquals(listOf(ranking), second)
    Mockito.verify(leaderboardService, Mockito.times(1))
        .findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
  }

  @Test
  fun shouldHandALateSubscriberTheLastRankingWithoutRecomputing() {
    val late = mutableListOf<List<RawLeaderboardEntry>>()
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
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
   * replayed to everyone joining later, until the next answer.
   */
  @Test
  fun shouldRecomputeRightAwayWhenARankedUserSetsAnAlias() {
    val renamed =
        listOf(ranking.single().copy(alias = RoomUserAliasProjection(USER_ID, null, "Beta", null)))
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(ranking)
        .then {
          Mockito.`when`(
                  leaderboardService.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null))
              .thenReturn(renamed)
          aliasEventPublisher.publishAliasChanged(RoomUserAliasChangedEvent(ROOM_ID, USER_ID))
        }
        .expectNext(renamed)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldIgnoreAliasesOfUsersWhoAreNotRanked() {
    StepVerifier.withVirtualTime { registry.leaderboardFlux(QUESTIONNAIRE_ID, null) }
        .expectSubscription()
        .then { publishAnswerChanged(QUESTIONNAIRE_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(ranking)
        .then {
          aliasEventPublisher.publishAliasChanged(
              RoomUserAliasChangedEvent(ROOM_ID, UUID.randomUUID()))
        }
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
        .then { publishAnswerChanged(OTHER_QUESTIONNAIRE_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verifyNoInteractions(leaderboardService)
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
