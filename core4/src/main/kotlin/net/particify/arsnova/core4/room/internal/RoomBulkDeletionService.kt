/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.transaction.Transactional
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RoomBulkDeletionService(
    val roomService: RoomServiceImpl,
    val membershipService: MembershipServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun deleteRoomsAndMembershipsByUserId(userId: UUID) {
    logger.debug("Deleting all rooms and memberships for user {}.", userId)
    val ownedIds = membershipService.findIdsByIdUserIdAndIsOwner(userId)
    ownedIds.forEach { roomService.deleteById(it.roomId!!) }
    membershipService.deleteAllByIdUserId(userId)
  }
}
