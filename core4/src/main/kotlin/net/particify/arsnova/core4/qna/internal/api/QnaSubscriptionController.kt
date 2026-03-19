/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class QnaSubscriptionController(
    private val qnaEventPublisher: QnaEventPublisher,
) {

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun qnaStateChanged(@Argument roomId: UUID): Flux<Qna> {
    return qnaEventPublisher.qnaStateChangedFlux().filter { it.roomId == roomId }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun activeQnaPostUpdated(@Argument roomId: UUID): Flux<Qna> {
    return qnaEventPublisher.activePostUpdatedFlux().filter { it.roomId == roomId }
  }
}
