/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.CorrectState
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class PostMutationController(
    private val postService: PostServiceImpl,
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.qnaId, 'Qna', 'create_post')")
  fun createQnaPost(@Argument input: CreatePostInput): Post {
    return postService.create(input.qnaId, input.body, input.tagIds)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'delete')")
  fun deleteQnaPost(@Argument id: UUID): UUID {
    return postService.delete(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'write')")
  fun updateQnaPostFavorite(@Argument id: UUID, @Argument favorite: Boolean): Post {
    return postService.updateFavorite(id, favorite)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'write')")
  fun updateQnaPostCorrect(@Argument id: UUID, @Argument correct: CorrectState): Post {
    return postService.updateCorrect(id, correct)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'write')")
  fun acceptQnaPost(@Argument id: UUID): Post {
    return postService.accept(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'write')")
  fun rejectQnaPost(@Argument id: UUID): Post {
    return postService.reject(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#qnaId, 'Qna', 'delete')")
  @Transactional
  fun deleteQnaPostsByQnaId(
      @Argument qnaId: UUID,
      @Argument moderationState: ModerationState?
  ): Int {
    return postService.deletePostsByQnaIdAndModerationState(qnaId, moderationState)
  }
}
