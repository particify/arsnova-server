/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.internal.VoteServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class VoteMutationController(private val voteService: VoteServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.postId, 'Post', 'read')")
  fun voteQnaPost(@Argument input: VoteInput, @AuthenticationPrincipal user: User): Post {
    require(input.value == 1 || input.value == -1) { "Vote value must be +1 or -1." }
    return voteService.vote(input.value, input.postId, user)
  }
}
