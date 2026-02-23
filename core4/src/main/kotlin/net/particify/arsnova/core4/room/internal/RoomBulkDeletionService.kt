/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.room.RoomRole
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.support.WindowIterator
import org.springframework.stereotype.Service

private const val DELETE_BATCH_SIZE = 10

@Service
class RoomBulkDeletionService(
    val roomService: RoomServiceImpl,
    val membershipService: MembershipServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun deleteRoomsAndMembershipsByUserId(userId: UUID) {
    logger.debug("Deleting all rooms and memberships for user {}.", userId)
    val memberships =
        WindowIterator.of {
              membershipService.findByIdUserId(userId, it, Limit.of(DELETE_BATCH_SIZE))
            }
            .startingAt(ScrollPosition.offset())
    memberships.forEachRemaining {
      if (it.role == RoomRole.OWNER) {
        roomService.deleteById(it.id?.roomId!!)
      } else {
        membershipService.deleteById(it.id!!)
      }
    }
  }
}
