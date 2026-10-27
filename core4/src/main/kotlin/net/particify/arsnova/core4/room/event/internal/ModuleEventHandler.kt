/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event.internal

import net.particify.arsnova.core4.room.internal.RoomBulkDeletionService
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.room.internal.RoomViolationReportTarget
import net.particify.arsnova.core4.user.event.UserDeletedEvent
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecidedEvent
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ModuleEventHandler(
    val roomBulkDeletionService: RoomBulkDeletionService,
    val roomService: RoomServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleUserDeleted(event: UserDeletedEvent) {
    roomBulkDeletionService.deleteRoomsAndMembershipsByUserId(event.id)
  }

  /** Reports outlive their targets, so the room may be gone already. */
  @EventListener
  fun handleViolationReportDecided(event: ViolationReportDecidedEvent) {
    if (event.targetType != RoomViolationReportTarget.TYPE ||
        event.decision != ViolationReportDecision.REMOVAL ||
        !roomService.existsById(event.targetId)) {
      return
    }
    logger.debug("Deleting room due to violation report decision: {}", event)
    roomService.deleteById(event.targetId)
  }
}
