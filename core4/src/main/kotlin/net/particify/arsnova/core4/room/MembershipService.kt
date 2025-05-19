/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window

interface MembershipService {
  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): Membership?

  fun findByUserId(userId: UUID, scrollPosition: ScrollPosition): Window<Membership>
}
