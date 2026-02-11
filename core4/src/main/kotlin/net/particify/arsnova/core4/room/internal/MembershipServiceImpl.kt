/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.exception.AccessDeniedException
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.MembershipNotFoundException
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.stereotype.Service

private const val LAST_ACTIVITY_THRESHOLD_MINUTES = 5L
private const val LAST_ACTIVITY_MINIMUM_DIFFERENCE_SECONDS = 5L

@Service
class MembershipServiceImpl(
    private val membershipRepository: MembershipRepository,
    roomProperties: RoomProperties
) : MembershipService, MembershipRepository by membershipRepository {
  val demoShortIds = roomProperties.demo.map { it.shortId }

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

  fun joinRoom(room: Room, user: User): Membership {
    var membership = findOneByRoomIdAndUserId(room.id!!, user.id!!)
    val persistedMembership =
        if (membership == null) {
          if (demoShortIds.contains(room.shortId)) {
            throw AccessDeniedException("Room cannot be joined.")
          }
          membership =
              Membership(
                  room = room,
                  user = user,
                  role = RoomRole.PARTICIPANT,
                  lastActivityAt = Instant.now())
          save(membership)
        } else if (membership.lastActivityAt == null ||
            membership.lastActivityAt!! <
                Instant.now().minus(Duration.ofSeconds(LAST_ACTIVITY_MINIMUM_DIFFERENCE_SECONDS))) {
          membership.lastActivityAt = Instant.now()
          save(membership)
        } else membership
    return persistedMembership
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Membership>) =
      membershipRepository.deleteInBatch(entities)
}
