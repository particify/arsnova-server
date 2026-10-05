/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.security.RoomRoleLookup
import org.springframework.stereotype.Component

@Component
class MembershipRoomRoleLookup(private val membershipService: MembershipService) : RoomRoleLookup {
  override fun findRoleName(roomId: UUID, userId: UUID): String? =
      membershipService.findOneByRoomIdAndUserId(roomId, userId)?.role?.name
}
