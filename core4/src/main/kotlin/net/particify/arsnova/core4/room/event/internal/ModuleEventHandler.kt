/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event.internal

import net.particify.arsnova.core4.room.internal.RoomBulkDeletionService
import net.particify.arsnova.core4.user.event.UserDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ModuleEventHandler(val roomBulkDeletionService: RoomBulkDeletionService) {
  @EventListener
  fun handleUserDeleted(event: UserDeletedEvent) {
    roomBulkDeletionService.deleteRoomsAndMembershipsByUserId(event.id)
  }
}
