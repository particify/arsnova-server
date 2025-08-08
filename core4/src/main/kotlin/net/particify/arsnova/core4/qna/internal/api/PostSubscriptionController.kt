/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.PostCountSummary
import net.particify.arsnova.core4.qna.internal.PostRepository
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class PostSubscriptionController(
    private val postEventPublisher: PostEventPublisher,
    private val postRepository: PostRepository,
    private val postServiceImpl: PostServiceImpl
) {

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostCreated(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postCreatedFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostDeleted(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postDeletedFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostAccepted(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postAcceptedFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostRejected(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postRejectedFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostReplied(@Argument qnaId: UUID): Flux<CreateReplyEvent> {
    return postEventPublisher.postReplyCreateFlux().filter {
      (postRepository.findById(it.postId)).get().qna!!.id == qnaId
    }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostMarkedFavorite(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postMarkedFavoriteFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostMarkedCorrect(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher.postMarkedCorrectFlux().filter { it.qna!!.id == qnaId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostVoted(@Argument qnaId: UUID): Flux<Post> {
    return postEventPublisher
        .postVotedFlux()
        .filter { it.qna!!.id == qnaId }
        .groupBy { it.id!! }
        .flatMap { groupedFlux -> groupedFlux.sample(Duration.ofSeconds(1)) }
  }

  @SubscriptionMapping
  fun qnaPostCountSummary(@Argument qnaId: UUID): Flux<PostCountSummary> {
    val initial = Mono.fromCallable { postServiceImpl.loadPostCountSummaryByQnaId(qnaId) }
    val updates =
        Flux.merge(
                postEventPublisher.postCreatedFlux(),
                postEventPublisher.postAcceptedFlux(),
                postEventPublisher.postRejectedFlux(),
                postEventPublisher.postDeletedFlux())
            .filter { it.qna!!.id!! == qnaId }
            .sample(Duration.ofSeconds(2))
            .flatMap { Mono.fromCallable { postServiceImpl.loadPostCountSummaryByQnaId(qnaId) } }
    return Flux.concat(initial, updates)
  }
}
