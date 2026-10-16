/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Holds one database-backed computation per key, shared by all subscribers of that key. Every
 * participant of a room may subscribe, and a computation of their own per sample interval each
 * exhausts the connection pool of the whole service.
 *
 * The emitted values are shared as well, so they must be complete rather than cut to a reader:
 * whatever depends on the subscriber is applied downstream, per subscriber, and has to gate the
 * value handed to a late joiner just like any other.
 *
 * @param triggers the changes of a key which call for a recomputation, sampled by [sampleInterval]
 */
class SharedFluxRegistry<K : Any, V : Any>(
    private val sampleInterval: Duration,
    private val triggers: (K) -> Flux<out Any>,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val sharedFluxes = ConcurrentHashMap<K, Flux<V>>()

  /**
   * Returns the stream of values for [key]. It starts with the current value, and a subscriber
   * joining an ongoing stream is handed the value computed last. [compute] blocks on the database,
   * so it runs off the event thread; it is only used by the first subscriber of a key, so it must
   * not depend on the subscriber. A null result emits nothing.
   */
  fun flux(key: K, compute: () -> V?): Flux<V> =
      sharedFluxes.computeIfAbsent(key) { createSharedFlux(it, compute) }

  /**
   * The pipeline unregisters itself once its last subscriber has left, by identity, so that a
   * replacement created for the same key in the meantime survives. The triggers are subscribed to
   * before the initial computation, so no change falls between the two.
   *
   * A failed initial computation reaches the subscribers, but the pipeline is unregistered before,
   * so the next subscriber starts over instead of being handed the failed one. A failed
   * recomputation, e.g. a transient database error, is skipped: ending the stream would end it for
   * every subscriber.
   */
  private fun createSharedFlux(key: K, compute: () -> V?): Flux<V> {
    val registered = AtomicReference<Flux<V>>()
    val unregister = { sharedFluxes.remove(key, registered.get()) }
    val sampledChanges: Flux<Boolean> =
        triggers(key).cast(Any::class.java).sample(sampleInterval).map { false }
    val shared =
        Flux.merge(sampledChanges, Mono.just(true))
            .concatMap { initial ->
              val computation =
                  Mono.defer { Mono.justOrEmpty<V>(compute()) }
                      .subscribeOn(Schedulers.boundedElastic())
              if (initial) computation else computation.onErrorResume { skipFailed(key, it) }
            }
            .doOnError { unregister() }
            .doFinally { unregister() }
            .replay(1)
            .refCount()
    registered.set(shared)
    return shared
  }

  private fun skipFailed(key: K, error: Throwable): Mono<V> {
    logger.warn("Skipped a failed computation for {}.", key, error)
    return Mono.empty()
  }
}
