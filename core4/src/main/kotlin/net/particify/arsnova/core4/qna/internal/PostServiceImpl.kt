/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.CorrectState
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.PostCountSummary
import net.particify.arsnova.core4.qna.Tag
import net.particify.arsnova.core4.qna.event.PostCreatedEvent
import net.particify.arsnova.core4.qna.event.PostDeletedEvent
import net.particify.arsnova.core4.qna.event.PostDuplicatedEvent
import net.particify.arsnova.core4.qna.event.PostsDeletedEvent
import net.particify.arsnova.core4.qna.exception.PostNotFoundException
import net.particify.arsnova.core4.qna.exception.QnaNotFoundException
import net.particify.arsnova.core4.qna.internal.api.PostEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private const val DELETE_CHUNK_SIZE = 100

@Service
class PostServiceImpl(
    private val postRepository: PostRepository,
    private val qnaRepository: QnaRepository,
    private val qnaServiceImpl: QnaServiceImpl,
    private val tagRepository: TagRepository,
    private val voteServiceImpl: VoteServiceImpl,
    private val replyServiceImpl: ReplyServiceImpl,
    private val postEventPublisher: PostEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

  @Transactional
  fun create(qnaId: UUID, body: String, tagIds: List<UUID>?): Post {
    val qna = qnaRepository.findByIdOrNull(qnaId) ?: throw QnaNotFoundException(qnaId)
    val tags = tagIds?.mapNotNull { tagRepository.findById(it).orElse(null) }?.toSet() ?: emptySet()
    val state = if (qna.autoPublish) ModerationState.ACCEPTED else ModerationState.REJECTED
    val post = Post(qna = qna, body = body)
    post.tags = tags.toMutableSet()
    post.moderationState = state
    val persistedPost = postRepository.save(post)
    postEventPublisher.publishPostCreated(persistedPost)
    applicationEventPublisher.publishEvent(PostCreatedEvent(persistedPost.id!!))
    return persistedPost
  }

  @Transactional
  fun delete(id: UUID): UUID {
    val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException()
    applicationEventPublisher.publishEvent(PostDeletedEvent(id))
    val qna =
        qnaRepository.findByIdOrNull(post.qna!!.id!!) ?: throw QnaNotFoundException(post.qna!!.id)
    if (qna.activePost != null && qna.activePost!!.id == id) {
      qnaServiceImpl.updateActivePost(qna.id!!, null)
    }
    postRepository.deleteById(id)
    postEventPublisher.publishPostDeleted(post)
    return id
  }

  @Transactional
  fun updateFavorite(id: UUID, favorite: Boolean): Post {
    val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException(id)
    post.favorite = favorite
    val persistedPost = postRepository.save(post)
    postEventPublisher.publishPostMarkedFavorite(persistedPost)
    return persistedPost
  }

  @Transactional
  fun updateCorrect(id: UUID, correct: CorrectState): Post {
    val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException(id)
    post.correct =
        when (correct) {
          CorrectState.CORRECT -> true
          CorrectState.WRONG -> false
          CorrectState.UNSET -> null
        }
    val persistedPost = postRepository.save(post)
    postEventPublisher.publishPostMarkedCorrect(persistedPost)
    return persistedPost
  }

  @Transactional
  fun accept(id: UUID): Post {
    val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException(id)
    post.moderationState = ModerationState.ACCEPTED
    val persistedPost = postRepository.save(post)
    postEventPublisher.publishPostAccepted(persistedPost)
    return persistedPost
  }

  @Transactional
  fun reject(id: UUID): Post {
    val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException(id)
    post.moderationState = ModerationState.REJECTED
    val persistedPost = postRepository.save(post)
    postEventPublisher.publishPostRejected(persistedPost)
    return persistedPost
  }

  @Transactional
  fun deletePostsByQnaIdAndModerationState(id: UUID, moderationState: ModerationState?): Int {
    return if (moderationState != null) {
      val postIds =
          postRepository.findByQnaIdAndModerationState(id, moderationState).map { it.id!! }
      deleteVotesAndRepliesByPostIds(postIds)
      applicationEventPublisher.publishEvent(PostsDeletedEvent(id, postIds.size))
      postRepository.deleteByQnaIdAndModerationState(id, moderationState)
    } else {
      deletePostsByQnaId(id)
    }
  }

  @Transactional
  fun deletePostsByQnaId(id: UUID): Int {
    val postIds = postRepository.findByQnaId(id).map { it.id!! }
    postIds.chunked(DELETE_CHUNK_SIZE).forEach { ids -> deleteVotesAndRepliesByPostIds(ids) }
    applicationEventPublisher.publishEvent(PostsDeletedEvent(id, postIds.size))
    return postRepository.deleteByQnaId(id)
  }

  private fun deleteVotesAndRepliesByPostIds(ids: List<UUID>) {
    ids.forEach { id ->
      replyServiceImpl.deleteByPostId(id)
      voteServiceImpl.deleteByPostId(id)
    }
  }

  @Transactional
  fun removeTagFromPosts(id: UUID): Int {
    return postRepository.removeTagFromPostsByTagId(id)
  }

  @Transactional
  fun loadPostCountSummaryByQnaId(qnaId: UUID): PostCountSummary {
    val accepted = postRepository.countByQnaIdAndModerationState(qnaId, ModerationState.ACCEPTED)
    val rejected = postRepository.countByQnaIdAndModerationState(qnaId, ModerationState.REJECTED)
    return PostCountSummary(accepted, rejected)
  }

  fun countAll(): Long {
    return postRepository.count()
  }

  @Transactional
  fun duplicateForQna(originalQnaId: UUID, duplicatedQnaId: UUID, duplicatedTags: List<Tag>) {
    val posts = postRepository.findByQnaId(originalQnaId)
    posts.forEach {
      val tags = duplicatedTags.filter { dt -> it.tags.map { t -> t.name }.contains(dt.name) }
      val newPost = it.copy(duplicatedQnaId, tags.toMutableSet())
      val newPostPersisted = postRepository.save(newPost)
      applicationEventPublisher.publishEvent(PostDuplicatedEvent(it.id!!, newPostPersisted.id!!))
    }
  }
}
