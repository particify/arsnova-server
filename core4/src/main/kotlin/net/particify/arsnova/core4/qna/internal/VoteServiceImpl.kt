/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.Vote
import net.particify.arsnova.core4.qna.event.VoteCreatedEvent
import net.particify.arsnova.core4.qna.event.VoteDeletedEvent
import net.particify.arsnova.core4.qna.event.VotesDeletedEvent
import net.particify.arsnova.core4.qna.exception.PostNotFoundException
import net.particify.arsnova.core4.qna.internal.api.PostEventPublisher
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VoteServiceImpl(
    private val voteRepository: VoteRepository,
    private val postRepository: PostRepository,
    private val userService: UserService,
    private val postEventPublisher: PostEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

  @Transactional
  fun vote(value: Int, postId: UUID, user: User): Post {
    val post = postRepository.findByIdOrNull(postId) ?: throw PostNotFoundException(postId)
    val existingVote = voteRepository.findByPostIdAndUserId(postId, user.id!!)
    val delta =
        if (existingVote != null) {
          if (existingVote.value == value) {
            voteRepository.delete(existingVote)
            applicationEventPublisher.publishEvent(VoteDeletedEvent(postId))
            -value
          } else {
            val delta = value - existingVote.value
            existingVote.value = value
            voteRepository.save(existingVote)
            delta
          }
        } else {
          voteRepository.save(Vote(post = Post(id = postId), user = user, value = value))
          applicationEventPublisher.publishEvent(VoteCreatedEvent(postId))
          value
        }
    post.score += delta
    postEventPublisher.publishPostVoted(post)
    return post
  }

  @Transactional
  fun deleteByPostId(postId: UUID): Int {
    val count = voteRepository.deleteByPostId(postId)
    applicationEventPublisher.publishEvent(VotesDeletedEvent(postId, count))
    return count
  }

  @Transactional
  fun duplicateForPost(originalPostId: UUID, duplicatedPostId: UUID) {
    val score = voteRepository.sumValueByPostId(originalPostId)
    if (score == 0) {
      return
    }
    val ghostUser = userService.getOrCreateGhostUser()
    voteRepository.save(
        Vote(
            id = Vote.PostUserId(duplicatedPostId, ghostUser.id),
            post = Post(id = duplicatedPostId),
            user = ghostUser,
            value = score))
    applicationEventPublisher.publishEvent(VoteCreatedEvent(duplicatedPostId))
  }
}
