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
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.slf4j.LoggerFactory
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
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val leaderboardServiceImpl: LeaderboardServiceImpl
) {
  companion object {
    private val LEADERBOARD_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  private data class LeaderboardKey(val questionnaireId: UUID, val contentId: UUID?)

  private val sharedFluxes = ConcurrentHashMap<LeaderboardKey, Flux<List<RawLeaderboardEntry>>>()

  /**
   * Returns the stream of rankings for the questionnaire. The stream starts with the current
   * ranking, and a subscriber joining an ongoing stream is handed the ranking computed last, so
   * neither has to wait for the next answer to see anything. The entries are unlocalized and
   * complete; paging and localization are per subscriber.
   */
  fun leaderboardFlux(questionnaireId: UUID, contentId: UUID?): Flux<List<RawLeaderboardEntry>> =
      sharedFluxes.computeIfAbsent(LeaderboardKey(questionnaireId, contentId)) {
        createSharedFlux(it)
      }

  /**
   * The pipeline unregisters itself once its last subscriber has left, so that questionnaires
   * nobody watches anymore do not accumulate. It removes itself by identity: a replacement created
   * for the same key in the meantime has to survive.
   */
  private fun createSharedFlux(key: LeaderboardKey): Flux<List<RawLeaderboardEntry>> {
    val registered = AtomicReference<Flux<List<RawLeaderboardEntry>>>()
    val shared =
        rankings(key).doFinally { sharedFluxes.remove(key, registered.get()) }.replay(1).refCount()
    registered.set(shared)
    return shared
  }

  /**
   * Answers and updates of the questionnaire trigger a sampled recomputation. The latter covers
   * publishing the solution, which reveals correctness and points of answers given before. The
   * events are subscribed to before the initial trigger is emitted, so no change falls between the
   * two.
   *
   * A failed computation, e.g. a transient database error or a content deleted meanwhile, is
   * skipped: ending the stream would end it for every subscriber.
   */
  private fun rankings(key: LeaderboardKey): Flux<List<RawLeaderboardEntry>> {
    val answerChanges: Flux<UUID> =
        roundStatsEventPublisher.answerChangedFlux().map { it.questionnaireId }
    val sampledChanges: Flux<UUID> =
        Flux.merge(answerChanges, questionnaireEventPublisher.questionnaireUpdatedFlux())
            .filter { it == key.questionnaireId }
            .sample(LEADERBOARD_UPDATE_SAMPLE_INTERVAL)
    return Flux.merge(sampledChanges, Mono.just(key.questionnaireId)).concatMap { _ ->
      findLeaderboard(key).onErrorResume { skipFailed(key, it) }
    }
  }

  private fun skipFailed(key: LeaderboardKey, error: Throwable): Mono<List<RawLeaderboardEntry>> {
    logger.warn("Skipped a failed leaderboard computation for {}.", key, error)
    return Mono.empty()
  }

  /** The read blocks on the database, so it is kept off the event thread. */
  private fun findLeaderboard(key: LeaderboardKey): Mono<List<RawLeaderboardEntry>> =
      Mono.fromCallable {
            leaderboardServiceImpl.findLeaderboardByQuestionnaireId(
                key.questionnaireId, key.contentId)
          }
          .subscribeOn(Schedulers.boundedElastic())
}
