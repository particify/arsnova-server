/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.Room
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.graphql.data.GraphQlRepository

@GraphQlRepository
interface RoomRepository : JpaRepository<Room, UUID> {
  fun findOneById(id: UUID): Room

  fun findOneByShortId(shortId: Int): Room

  fun countByShortId(shortId: Int): Int
}
