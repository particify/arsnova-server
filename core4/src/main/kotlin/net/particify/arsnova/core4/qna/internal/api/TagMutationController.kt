/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Tag
import net.particify.arsnova.core4.qna.internal.TagServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class TagMutationController(
    private val tagService: TagServiceImpl,
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.qnaId, 'Qna', 'write')")
  fun createQnaTag(@Argument input: CreateTagInput): Tag {
    return tagService.create(input.toTag())
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Tag', 'delete')")
  fun deleteQnaTag(@Argument id: UUID): UUID {
    tagService.delete(id)
    return id
  }
}
