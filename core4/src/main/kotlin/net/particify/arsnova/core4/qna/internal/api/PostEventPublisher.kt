/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.Reply
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues

@Suppress("TooManyFunctions")
@Component
class PostEventPublisher {
  private val createPostSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val deletePostSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val acceptPostSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val rejectPostSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val createReplySink =
      Sinks.many()
          .multicast()
          .onBackpressureBuffer<CreateReplyEvent>(Queues.SMALL_BUFFER_SIZE, false)
  private val markedPostFavoriteSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val markedPostCorrectSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)
  private val votePostSink =
      Sinks.many().multicast().onBackpressureBuffer<Post>(Queues.SMALL_BUFFER_SIZE, false)

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun publishPostCreated(post: Post) {
    createPostSink.tryEmitNext(post)
  }

  fun postCreatedFlux(): Flux<Post> {
    return createPostSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostDeleted(post: Post) {
    deletePostSink.tryEmitNext(post)
  }

  fun postDeletedFlux(): Flux<Post> {
    return deletePostSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostAccepted(post: Post) {
    acceptPostSink.tryEmitNext(post)
  }

  fun postAcceptedFlux(): Flux<Post> {
    return acceptPostSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostRejected(post: Post) {
    rejectPostSink.tryEmitNext(post)
  }

  fun postRejectedFlux(): Flux<Post> {
    return rejectPostSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishReplyCreate(postId: UUID, reply: Reply) {
    createReplySink.tryEmitNext(CreateReplyEvent(postId, reply))
  }

  fun postReplyCreateFlux(): Flux<CreateReplyEvent> {
    return createReplySink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostMarkedFavorite(post: Post) {
    markedPostFavoriteSink.tryEmitNext(post)
  }

  fun postMarkedFavoriteFlux(): Flux<Post> {
    return markedPostFavoriteSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostMarkedCorrect(post: Post) {
    markedPostCorrectSink.tryEmitNext(post)
  }

  fun postMarkedCorrectFlux(): Flux<Post> {
    return markedPostCorrectSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun publishPostVoted(post: Post) {
    votePostSink.tryEmitNext(post)
  }

  fun postVotedFlux(): Flux<Post> {
    return votePostSink.asFlux().doOnError { logger.error(it.message) }
  }
}
