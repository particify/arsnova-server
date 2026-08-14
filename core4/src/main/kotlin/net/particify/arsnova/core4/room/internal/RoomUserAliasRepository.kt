/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.RoomUserAlias
import org.springframework.data.jpa.repository.JpaRepository

interface RoomUserAliasRepository : JpaRepository<RoomUserAlias, UUID> {
  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias?

  fun findByRoomId(roomId: UUID): List<RoomUserAlias>

  fun deleteAllByRoomId(roomId: UUID): Int

  fun deleteAllByUserId(userId: UUID): Int
}
