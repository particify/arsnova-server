/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import net.particify.arsnova.core4.qna.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VoteRepository : JpaRepository<Vote, Vote.PostUserId> {
  fun findByPostIdAndUserId(postId: UUID, userId: UUID): Vote?

  @Modifying
  @Query("DELETE FROM Vote v WHERE v.id.postId = :postId")
  fun deleteByPostId(postId: UUID): Int

  fun countByPostId(postId: UUID): Int
}
