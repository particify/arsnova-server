/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.internal.QnaRepository
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class QnaQueryController(
    private val qnaRepository: QnaRepository,
) {

  @QueryMapping fun qna(): Map<Any, Any> = mapOf()

  @QueryMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  @Transactional(readOnly = true)
  fun qnasByRoomId(@Argument roomId: UUID, subrange: ScrollSubrange): Window<Qna> {
    return qnaRepository.findByRoomId(roomId, subrange.position().orElse(ScrollPosition.offset()))
  }
}
