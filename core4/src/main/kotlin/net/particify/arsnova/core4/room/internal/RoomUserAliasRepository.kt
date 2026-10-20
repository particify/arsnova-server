/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.RoomUserAlias
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RoomUserAliasRepository : JpaRepository<RoomUserAlias, UUID> {
  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias?

  fun findByRoomId(roomId: UUID): List<RoomUserAlias>

  @Modifying
  @Query("DELETE FROM RoomUserAlias a WHERE a.room.id = :roomId")
  fun deleteAllByRoomId(roomId: UUID): Int

  @Modifying
  @Query("DELETE FROM RoomUserAlias a WHERE a.user.id = :userId")
  fun deleteAllByUserId(userId: UUID): Int
}
