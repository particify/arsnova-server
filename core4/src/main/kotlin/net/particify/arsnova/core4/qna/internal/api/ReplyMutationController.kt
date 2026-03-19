/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Reply
import net.particify.arsnova.core4.qna.internal.ReplyServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class ReplyMutationController(
    private val replyService: ReplyServiceImpl,
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.postId, 'Post', 'write')")
  fun createQnaReply(@Argument input: CreateReplyInput): Reply {
    return replyService.create(input.toReply())
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.postId, 'Post', 'write')")
  fun updateQnaReply(@Argument input: UpdateReplyInput): Reply {
    return replyService.update(input.toReply())
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Post', 'delete')")
  fun deleteQnaReply(@Argument id: UUID): UUID {
    replyService.delete(id)
    return id
  }
}
