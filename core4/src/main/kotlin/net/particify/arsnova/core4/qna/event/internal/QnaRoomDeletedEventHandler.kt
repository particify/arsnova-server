/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.internal.QnaServiceImpl
import net.particify.arsnova.core4.room.event.RoomDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QnaRoomDeletedEventHandler(
    private val qnaService: QnaServiceImpl,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleRoomDeleted(event: RoomDeletedEvent) {
    logger.debug("Deleting qnas due to room deletion: {}", event)
    qnaService.deleteByRoomId(event.id)
  }
}
