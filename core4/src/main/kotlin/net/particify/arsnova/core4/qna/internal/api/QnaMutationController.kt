/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.internal.QnaServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class QnaMutationController(private val qnaService: QnaServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun startQna(@Argument id: UUID): Qna {
    return qnaService.start(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun pauseQna(@Argument id: UUID): Qna {
    return qnaService.pause(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun stopQna(@Argument id: UUID): Qna {
    return qnaService.stop(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun updateQnaThreshold(@Argument id: UUID, @Argument threshold: Int?): Qna {
    return qnaService.updateThreshold(id, threshold)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun updateQnaAutoPublish(@Argument id: UUID, @Argument autoPublish: Boolean): Qna {
    return qnaService.updateAutoPublish(id, autoPublish)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Qna', 'write')")
  fun updateQnaActivePostId(@Argument id: UUID, @Argument activePostId: UUID?): Qna {
    return qnaService.updateActivePost(id, activePostId)
  }
}
