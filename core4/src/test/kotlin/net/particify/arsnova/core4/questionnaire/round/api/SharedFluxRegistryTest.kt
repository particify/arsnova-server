/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.dao.TransientDataAccessResourceException
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier

private const val KEY = "key"
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class SharedFluxRegistryTest {
  private val changes = Sinks.many().multicast().directBestEffort<Unit>()
  private val ends = Sinks.many().multicast().directBestEffort<Throwable>()
  private val registry =
      SharedFluxRegistry<String, Int>(SAMPLE_INTERVAL, ends = { ends.asFlux() }) {
        changes.asFlux()
      }

  /** A computation which returns the given results in turn, throwing those which are errors. */
  private fun computation(vararg results: Any): () -> Int? {
    val remaining = ArrayDeque(results.toList())
    return {
      when (val result = remaining.removeFirst()) {
        is Throwable -> throw result
        else -> result as Int
      }
    }
  }

  @Test
  fun shouldSkipAFailedRecomputationAndKeepServingEverySubscriber() {
    val compute = computation(1, TransientDataAccessResourceException("Connection lost"), 3)
    val second = mutableListOf<Int>()
    StepVerifier.withVirtualTime { registry.flux(KEY, compute) }
        .expectSubscription()
        .expectNext(1)
        .then {
          registry.flux(KEY, compute).subscribe { second.add(it) }
          changes.tryEmitNext(Unit)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNoEvent(SAMPLE_INTERVAL)
        .then { changes.tryEmitNext(Unit) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(3)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(1, 3), second)
  }

  @Test
  fun shouldNotHandOnAStreamWhoseInitialComputationFailed() {
    val compute = computation(TransientDataAccessResourceException("Connection lost"), 2)
    val failed = registry.flux(KEY, compute)
    // A subscriber reacting to the error right away must not be handed the failed stream either.
    val handedOnError = AtomicReference<Flux<Int>>()
    StepVerifier.create(failed.doOnError { handedOnError.set(registry.flux(KEY, compute)) })
        .expectError(TransientDataAccessResourceException::class.java)
        .verify(VERIFY_TIMEOUT)

    Assertions.assertNotSame(failed, handedOnError.get())
    StepVerifier.create(registry.flux(KEY, compute))
        .expectNext(2)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEndEverySubscriberAndStartOverOnceTheKeyEnds() {
    val compute = computation(1, 2)
    val second = mutableListOf<Int>()
    var secondError: Throwable? = null
    StepVerifier.create(registry.flux(KEY, compute))
        .expectNext(1)
        .then {
          registry.flux(KEY, compute).subscribe({ second.add(it) }, { secondError = it })
          ends.tryEmitNext(IllegalStateException("Deleted"))
        }
        .expectErrorMessage("Deleted")
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(1), second)
    Assertions.assertEquals("Deleted", secondError?.message)
    StepVerifier.create(registry.flux(KEY, compute))
        .expectNext(2)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }
}
