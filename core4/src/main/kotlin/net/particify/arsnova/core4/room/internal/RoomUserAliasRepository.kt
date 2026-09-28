/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.RoomUserAliasProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RoomUserAliasRepository : JpaRepository<RoomUserAlias, UUID> {
  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias?

  fun findByRoomId(roomId: UUID): List<RoomUserAlias>

  /**
   * Reads the room's aliases without their associated entities. Both of those are eagerly mapped,
   * so loading the aliases themselves would cost an additional query per participant.
   */
  @Query(
      "SELECT new net.particify.arsnova.core4.room.RoomUserAliasProjection(" +
          "a.user.id, a.id, a.alias, a.seed) " +
          "FROM RoomUserAlias a WHERE a.room.id = :roomId AND a.user.id IS NOT NULL")
  fun findProjectedByRoomId(roomId: UUID): List<RoomUserAliasProjection>

  @Modifying
  @Query("DELETE FROM RoomUserAlias a WHERE a.room.id = :roomId")
  fun deleteAllByRoomId(roomId: UUID): Int

  @Modifying
  @Query("DELETE FROM RoomUserAlias a WHERE a.user.id = :userId")
  fun deleteAllByUserId(userId: UUID): Int
}
