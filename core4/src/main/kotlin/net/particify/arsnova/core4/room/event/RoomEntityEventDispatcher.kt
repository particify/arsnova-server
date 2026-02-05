/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import jakarta.persistence.PostUpdate
import net.particify.arsnova.core4.common.event.EntityChangeEvent
import net.particify.arsnova.core4.room.Room
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class RoomEntityEventDispatcher(private val applicationEventPublisher: ApplicationEventPublisher) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @PostUpdate
  fun handlePostUpdate(room: Room) {
    val event = EntityChangeEvent(Room::class, EntityChangeEvent.ChangeType.UPDATE, room.id!!)
    logger.debug("Dispatching entity update event for room: {}", event)
    applicationEventPublisher.publishEvent(event)
  }
}
