/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues

/** Signals that a user's alias for a room was set, so views showing it can refresh. */
data class RoomUserAliasChangedEvent(val roomId: UUID, val userId: UUID)

@Component
class RoomUserAliasEventPublisher {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val aliasChangedSink =
      Sinks.many()
          .multicast()
          .onBackpressureBuffer<RoomUserAliasChangedEvent>(Queues.SMALL_BUFFER_SIZE, false)

  /** Emitted after the commit, so that subscribers reading the alias see the new one. */
  fun publishAliasChanged(event: RoomUserAliasChangedEvent) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          object : TransactionSynchronization {
            override fun afterCommit() = emit(event)
          })
    } else {
      emit(event)
    }
  }

  private fun emit(event: RoomUserAliasChangedEvent) {
    val result = aliasChangedSink.tryEmitNext(event)
    if (result.isFailure) logger.warn("Dropped alias changed event {}: {}", event, result)
  }

  fun aliasChangedFlux(): Flux<RoomUserAliasChangedEvent> =
      aliasChangedSink.asFlux().doOnError { logger.error(it.message) }
}
