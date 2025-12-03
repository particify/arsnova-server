/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.MembershipService
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.stereotype.Service

@Service
class MembershipServiceImpl(private val membershipRepository: MembershipRepository) :
    MembershipService, MembershipRepository by membershipRepository {
  override fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): Membership? {
    return membershipRepository.findOneByRoomIdAndUserId(roomId, userId)
  }

  override fun findByUserId(userId: UUID, scrollPosition: ScrollPosition): Window<Membership> {
    return membershipRepository.findByUserId(userId, scrollPosition)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Membership>) =
      membershipRepository.deleteInBatch(entities)
}
