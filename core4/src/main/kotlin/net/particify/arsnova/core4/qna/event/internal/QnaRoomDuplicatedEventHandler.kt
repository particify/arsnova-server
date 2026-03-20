/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.internal.QnaServiceImpl
import net.particify.arsnova.core4.room.event.RoomDuplicatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QnaRoomDuplicatedEventHandler(
    private val qnaService: QnaServiceImpl,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleRoomDuplicated(event: RoomDuplicatedEvent) {
    logger.debug("Duplicate qnas due to room duplication: {}", event)
    qnaService.duplicateForRoom(event.originalRoomId, event.duplicatedRoomId)
  }
}
