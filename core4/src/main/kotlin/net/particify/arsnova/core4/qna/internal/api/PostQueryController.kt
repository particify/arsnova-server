/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import com.querydsl.core.BooleanBuilder
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.qna.CorrectState
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.PostCountSummary
import net.particify.arsnova.core4.qna.PostSortOrder
import net.particify.arsnova.core4.qna.QPost
import net.particify.arsnova.core4.qna.internal.PostRepository
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import net.particify.arsnova.core4.qna.internal.VoteRepository
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class PostQueryController(
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val postServiceImpl: PostServiceImpl
) {

  companion object {
    const val DEFAULT_QUERY_LIMIT = 20
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'read')")
  fun qnaPostCountsByQnaId(@Argument qnaId: UUID): PostCountSummary {
    return postServiceImpl.loadPostCountSummaryByQnaId(qnaId)
  }

  private fun buildPostFilter(input: PostQueryInput): BooleanBuilder {
    val qPost = QPost.post
    val builder = BooleanBuilder()
    builder.and(qPost.qna.id.eq(input.qnaId))
    builder.and(qPost.qna.threshold.isNull.or(qPost.score.goe(qPost.qna.threshold)))

    input.let {
      builder.and(qPost.moderationState.eq(it.moderationState ?: ModerationState.ACCEPTED))
      applyTimeRangeFilter(qPost, builder, it.period)
      it.favorite?.let { f -> builder.and(qPost.favorite.eq(f)) }
      it.correct?.let { c ->
        builder.and(
            when (c) {
              CorrectState.CORRECT -> qPost.correct.eq(true)
              CorrectState.WRONG -> qPost.correct.eq(false)
              CorrectState.UNSET -> qPost.correct.isNull
            })
      }
      it.replied?.let { r ->
        builder.and(if (r) qPost.replies.isNotEmpty else qPost.replies.isEmpty)
      }
      it.tagIds
          ?.takeIf { list -> list.isNotEmpty() }
          ?.let { tags -> builder.and(qPost.tags.any().id.`in`(tags)) }
      it.search
          ?.takeIf { s -> s.isNotBlank() }
          ?.let { s -> builder.and(qPost.body.containsIgnoreCase(s)) }
    }
    return builder
  }

  private fun applyTimeRangeFilter(qPost: QPost, builder: BooleanBuilder, period: Long?) {
    val cutoff = period?.let { Instant.now().minus(Duration.ofHours(it)) }
    cutoff?.let { builder.and(qPost.auditMetadata.createdAt.goe(it)) }
  }

  private fun toSort(sortOrder: PostSortOrder?): Sort {
    return when (sortOrder) {
      PostSortOrder.HIGHEST_SCORE -> Sort.by(Sort.Order.desc("score"))
      PostSortOrder.LOWEST_SCORE -> Sort.by(Sort.Order.asc("score"))
      else -> Sort.unsorted()
    }
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#query.qnaId, 'Qna', 'read')")
  fun qnaPostsByQnaId(@Argument query: PostQueryInput, subrange: ScrollSubrange): Window<Post> {
    val builder = buildPostFilter(query)
    val sort = toSort(query.sortOrder).and(Sort.by(Sort.Order.desc("auditMetadata.createdAt")))
    return postRepository.findBy(builder) { q ->
      q.sortBy(sort)
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @SchemaMapping(typeName = "Post")
  fun createdAt(post: Post): OffsetDateTime? {
    return post.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Post")
  fun userVote(post: Post, @AuthenticationPrincipal user: User?): Int {
    if (user == null) {
      return 0
    }
    val vote = voteRepository.findByPostIdAndUserId(post.id!!, user.id!!)
    return vote?.value ?: 0
  }

  @SchemaMapping(typeName = "Post")
  fun correct(post: Post): CorrectState {
    return when (post.correct) {
      true -> CorrectState.CORRECT
      false -> CorrectState.WRONG
      null -> CorrectState.UNSET
    }
  }
}
