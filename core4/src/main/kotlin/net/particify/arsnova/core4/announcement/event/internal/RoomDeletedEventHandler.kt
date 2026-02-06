/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.event.internal

import net.particify.arsnova.core4.announcement.event.AnnouncementsDeletedEvent
import net.particify.arsnova.core4.announcement.internal.AnnouncementServiceImpl
import net.particify.arsnova.core4.room.event.RoomDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RoomDeletedEventHandler(
    val announcementService: AnnouncementServiceImpl,
    val applicationEventPublisher: ApplicationEventPublisher
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleRoomDeleted(event: RoomDeletedEvent) {
    logger.debug("Deleting announcements due to room deletion: {}", event)
    val count = announcementService.deleteByRoomId(event.entityId)
    applicationEventPublisher.publishEvent(AnnouncementsDeletedEvent(event.entityId, count))
  }
}
