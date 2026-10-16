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
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.room.RoomUserAliasChangedEvent
import net.particify.arsnova.core4.room.RoomUserAliasEventPublisher
import org.springframework.data.repository.findByIdOrNull
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
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val leaderboardServiceImpl: LeaderboardServiceImpl,
    private val questionnaireRepository: QuestionnaireRepository
) {
  companion object {
    private val LEADERBOARD_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

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
        findRoomId(key.questionnaireId)
            .flatMapMany { roomId -> rankings(key, roomId) }
            .doFinally { sharedFluxes.remove(key, registered.get()) }
            .replay(1)
            .refCount()
    registered.set(shared)
    return shared
  }

  /**
   * Answers and updates of the questionnaire trigger a sampled recomputation. The latter covers
   * publishing the solution, which reveals correctness and points of answers given before.
   *
   * Besides those, an alias set in the questionnaire's room by a ranked user triggers a
   * recomputation, right away rather than sampled: otherwise the ranking replayed to every
   * subscriber joining later would keep showing the user anonymously until the next answer. Users
   * who are not ranked yet appear with their alias once they answer.
   *
   * Each trigger decides whether to recompute only once the computations before it are done, so an
   * alias set while the initial ranking is read is checked against that ranking. The events are
   * subscribed to before the initial trigger is emitted, so no change falls between the two.
   */
  private fun rankings(key: LeaderboardKey, roomId: UUID): Flux<List<RawLeaderboardEntry>> {
    val lastRanking = AtomicReference<List<RawLeaderboardEntry>>(emptyList())
    val answerChanges: Flux<UUID> =
        roundStatsEventPublisher.answerChangedFlux().map { it.questionnaireId }
    val sampledChanges: Flux<() -> Boolean> =
        Flux.merge(answerChanges, questionnaireEventPublisher.questionnaireUpdatedFlux())
            .filter { it == key.questionnaireId }
            .sample(LEADERBOARD_UPDATE_SAMPLE_INTERVAL)
            .map { { true } }
    val aliasChanges: Flux<() -> Boolean> =
        roomUserAliasEventPublisher
            .aliasChangedFlux()
            .filter { event: RoomUserAliasChangedEvent -> event.roomId == roomId }
            .map { event -> { lastRanking.get().any { it.userId == event.userId } } }
    val initial: Mono<() -> Boolean> = Mono.just { true }
    return Flux.merge(sampledChanges, aliasChanges, initial)
        .concatMap { recompute -> if (recompute()) findLeaderboard(key) else Mono.empty() }
        .doOnNext { lastRanking.set(it) }
  }

  private fun findRoomId(questionnaireId: UUID): Mono<UUID> =
      Mono.fromCallable {
            questionnaireRepository.findByIdOrNull(questionnaireId)?.roomId
                ?: throw QuestionnaireNotFoundException(questionnaireId)
          }
          .subscribeOn(Schedulers.boundedElastic())

  /** The read blocks on the database, so it is kept off the event thread. */
  private fun findLeaderboard(key: LeaderboardKey): Mono<List<RawLeaderboardEntry>> =
      Mono.fromCallable {
            leaderboardServiceImpl.findLeaderboardByQuestionnaireId(
                key.questionnaireId, key.contentId)
          }
          .subscribeOn(Schedulers.boundedElastic())
}
