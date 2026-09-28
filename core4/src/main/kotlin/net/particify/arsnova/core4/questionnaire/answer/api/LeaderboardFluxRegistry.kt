/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.RawLeaderboardEntry
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.room.RoomUserAliasEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Holds one leaderboard computation per questionnaire and content, shared by all of its
 * subscribers. A room can hold a four-digit number of participants, and giving each of them their
 * own database-backed recomputation exhausts the connection pool, which takes down every other
 * request of the service along with the leaderboard.
 */
@Component
class LeaderboardFluxRegistry(
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val roomUserAliasEventPublisher: RoomUserAliasEventPublisher,
    private val leaderboardServiceImpl: LeaderboardServiceImpl
) {
  companion object {
    private val LEADERBOARD_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  private data class LeaderboardKey(val questionnaireId: UUID, val contentId: UUID?)

  private val sharedFluxes = ConcurrentHashMap<LeaderboardKey, Flux<List<RawLeaderboardEntry>>>()

  /**
   * Returns the stream of rankings for the questionnaire. A subscriber joining an ongoing stream is
   * handed the ranking computed last, so it does not have to wait for the next answer to see
   * anything. The entries are unlocalized and complete; paging and localization are per subscriber.
   */
  fun leaderboardFlux(questionnaireId: UUID, contentId: UUID?): Flux<List<RawLeaderboardEntry>> =
      sharedFluxes.computeIfAbsent(LeaderboardKey(questionnaireId, contentId)) {
        createSharedFlux(it)
      }

  /**
   * The pipeline unregisters itself once its last subscriber has left, so that questionnaires
   * nobody watches anymore do not accumulate. It removes itself by identity: a replacement created
   * for the same key in the meantime has to survive.
   *
   * Besides answers, an alias set by a ranked user triggers a recomputation, right away rather than
   * sampled: otherwise the ranking replayed to every subscriber joining later would keep showing
   * the user anonymously until the next answer. Users who are not ranked yet appear with their
   * alias once they answer.
   */
  private fun createSharedFlux(key: LeaderboardKey): Flux<List<RawLeaderboardEntry>> {
    val registered = AtomicReference<Flux<List<RawLeaderboardEntry>>>()
    val lastRanking = AtomicReference<List<RawLeaderboardEntry>>(emptyList())
    val answerChanges =
        roundStatsEventPublisher
            .answerChangedFlux()
            .filter { event: AnswerChangedEvent -> event.questionnaireId == key.questionnaireId }
            .sample(LEADERBOARD_UPDATE_SAMPLE_INTERVAL)
            .map {}
    val aliasChanges =
        roomUserAliasEventPublisher
            .aliasChangedFlux()
            .filter { event -> lastRanking.get().any { it.userId == event.userId } }
            .map {}
    val shared =
        Flux.merge(answerChanges, aliasChanges)
            .concatMap { findLeaderboard(key) }
            .doOnNext { lastRanking.set(it) }
            .doFinally { sharedFluxes.remove(key, registered.get()) }
            .replay(1)
            .refCount()
    registered.set(shared)
    return shared
  }

  /** The read blocks on the database, so it is kept off the event thread. */
  private fun findLeaderboard(key: LeaderboardKey): Mono<List<RawLeaderboardEntry>> =
      Mono.fromCallable {
            leaderboardServiceImpl.findLeaderboardByQuestionnaireId(
                key.questionnaireId, key.contentId)
          }
          .subscribeOn(Schedulers.boundedElastic())
}
