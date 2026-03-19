/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import net.particify.arsnova.core4.qna.Qna
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues

@Component
class QnaEventPublisher {
  private val updateQnaStateSink =
      Sinks.many().multicast().onBackpressureBuffer<Qna>(Queues.SMALL_BUFFER_SIZE, false)
  private val updateActivePostSink =
      Sinks.many().multicast().onBackpressureBuffer<Qna>(Queues.SMALL_BUFFER_SIZE, false)

  fun publishQnaStateChanged(qna: Qna) {
    updateQnaStateSink.tryEmitNext(qna)
  }

  fun qnaStateChangedFlux(): Flux<Qna> {
    return updateQnaStateSink.asFlux()
  }

  fun publishUpdatedActivePost(qna: Qna) {
    updateActivePostSink.tryEmitNext(qna)
  }

  fun activePostUpdatedFlux(): Flux<Qna> {
    return updateActivePostSink.asFlux()
  }
}
