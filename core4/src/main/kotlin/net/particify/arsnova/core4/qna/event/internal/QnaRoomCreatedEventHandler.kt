/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.internal.QnaServiceImpl
import net.particify.arsnova.core4.room.event.RoomCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QnaRoomCreatedEventHandler(
    private val qnaService: QnaServiceImpl,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleRoomCreated(event: RoomCreatedEvent) {
    logger.debug("Create default qna due to room creation: {}", event)
    qnaService.create(event.id)
  }
}
