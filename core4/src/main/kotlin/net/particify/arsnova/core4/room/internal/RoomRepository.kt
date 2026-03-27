/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.Room
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface RoomRepository : JpaRepository<Room, UUID>, QuerydslPredicateExecutor<Room> {
  fun findOneByShortId(shortId: Int): Room?

  fun countByShortId(shortId: Int): Int
}
