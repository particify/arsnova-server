/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.MembershipNotFoundException
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.stereotype.Service

private const val LAST_ACTIVITY_THRESHOLD_MINUTES = 5L

@Service
class MembershipServiceImpl(private val membershipRepository: MembershipRepository) :
    MembershipService, MembershipRepository by membershipRepository {
  override fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): Membership? {
    return membershipRepository.findOneByRoomIdAndUserId(roomId, userId)
  }

  override fun findByUserId(userId: UUID, scrollPosition: ScrollPosition): Window<Membership> {
    return membershipRepository.findByUserId(userId, scrollPosition)
  }

  fun countActiveMembersByRoomIds(roomIds: List<UUID>): Map<UUID, Int> {
    val counts =
        countByRoomIdsAndLastActivityAtAfter(
            roomIds, Instant.now().minus(Duration.ofMinutes(LAST_ACTIVITY_THRESHOLD_MINUTES)))
    return counts.associate {
      Pair(it.get(0, UUID::class.java), it.get(1, Long::class.java).toInt())
    }
  }

  fun findOwnerMembershipByRoomId(roomId: UUID): Membership? {
    return membershipRepository.findOneByRoomIdAndRole(roomId, RoomRole.OWNER)
        ?: throw MembershipNotFoundException()
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Membership>) =
      membershipRepository.deleteInBatch(entities)
}
