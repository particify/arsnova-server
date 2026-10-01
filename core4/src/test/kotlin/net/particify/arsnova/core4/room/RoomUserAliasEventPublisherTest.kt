/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.time.Duration
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import reactor.test.StepVerifier

private val EVENT = RoomUserAliasChangedEvent(UUID.randomUUID(), UUID.randomUUID())
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)
private val NO_EVENT_WINDOW = Duration.ofMillis(200)

class RoomUserAliasEventPublisherTest {
  private val publisher = RoomUserAliasEventPublisher()

  @Test
  fun shouldEmitRightAwayOutsideATransaction() {
    StepVerifier.create(publisher.aliasChangedFlux())
        .then { publisher.publishAliasChanged(EVENT) }
        .expectNext(EVENT)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** Subscribers recompute from the database, so they must not see the event before the commit. */
  @Test
  fun shouldEmitOnlyOnceTheTransactionCommitted() {
    TransactionSynchronizationManager.initSynchronization()
    try {
      StepVerifier.create(publisher.aliasChangedFlux())
          .then { publisher.publishAliasChanged(EVENT) }
          .expectNoEvent(NO_EVENT_WINDOW)
          .then {
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
          }
          .expectNext(EVENT)
          .thenCancel()
          .verify(VERIFY_TIMEOUT)
    } finally {
      TransactionSynchronizationManager.clearSynchronization()
    }
  }
}
