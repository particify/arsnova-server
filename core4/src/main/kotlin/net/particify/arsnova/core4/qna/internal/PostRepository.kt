/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface PostRepository : JpaRepository<Post, UUID>, QuerydslPredicateExecutor<Post> {
  fun countByQnaIdAndModerationState(qnaId: UUID, moderationState: ModerationState): Int

  fun findByQnaIdAndModerationState(qnaId: UUID, moderationState: ModerationState): List<Post>

  fun findByQnaId(qnaId: UUID): List<Post>

  fun deleteByQnaIdAndModerationState(qnaId: UUID, moderationState: ModerationState): Int

  fun deleteByQnaId(qnaId: UUID): Int

  @Modifying
  @Query(value = "DELETE FROM qna.post_tag WHERE tag_id = :tagId", nativeQuery = true)
  fun removeTagFromPostsByTagId(tagId: UUID): Int
}
