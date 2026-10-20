/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues

/**
 * Signals that the answer set of a round changed in a way subscribers need to see. Raised both when
 * an answer is submitted and when one is hidden or unhidden, since hidden answers are excluded from
 * derived data like a content's answer count and its round stats.
 */
data class AnswerChangedEvent(
    val roundId: UUID,
    val contentId: UUID,
    val questionnaireId: UUID,
)

@Component
class RoundStatsEventPublisher {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val answerChangedSink =
      Sinks.many()
          .multicast()
          .onBackpressureBuffer<AnswerChangedEvent>(Queues.SMALL_BUFFER_SIZE, false)

  /** A deletion nobody follows concerns nobody, so it is neither buffered nor reported. */
  private val roundDeletedSink = Sinks.many().multicast().directBestEffort<UUID>()

  /**
   * Emits once the current transaction has committed, as subscribers react by reading the changed
   * data right away: emitted before, they could read the state without the change and, with nothing
   * left to trigger another read, keep showing it. Without a transaction it is emitted right away.
   *
   * A full buffer or a concurrent emission makes the sink reject the event. Subscribers only ever
   * re-read derived data, so a dropped event costs an update rather than consistency, but it stays
   * worth knowing about since it means the emission rate outgrew the buffer.
   */
  fun publishAnswerChanged(event: AnswerChangedEvent) {
    afterCommit { emit(answerChangedSink, event, "answer changed", event.roundId) }
  }

  /**
   * Signals that a round was deleted, so that whoever follows it can let go of it. Emitted once the
   * current transaction has committed, like [publishAnswerChanged].
   */
  fun publishRoundDeleted(roundId: UUID) {
    afterCommit { emit(roundDeletedSink, roundId, "round deleted", roundId) }
  }

  private fun afterCommit(action: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          object : TransactionSynchronization {
            override fun afterCommit() = action()
          })
    } else {
      action()
    }
  }

  private fun <T : Any> emit(sink: Sinks.Many<T>, event: T, name: String, roundId: UUID) {
    val result = sink.tryEmitNext(event)
    if (result.isFailure && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
      logger.warn("Dropped {} event for round {}: {}", name, roundId, result)
    }
  }

  fun answerChangedFlux(): Flux<AnswerChangedEvent> {
    return answerChangedSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun roundDeletedFlux(): Flux<UUID> {
    return roundDeletedSink.asFlux().doOnError { logger.error(it.message) }
  }
}
