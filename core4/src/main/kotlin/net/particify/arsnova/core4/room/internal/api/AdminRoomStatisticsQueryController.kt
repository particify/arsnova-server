/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import net.particify.arsnova.core4.room.AdminRoomStats
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminRoomStatisticsQueryController(
    private val roomService: RoomServiceImpl,
) {

  @QueryMapping
  fun adminRoomStats(): AdminRoomStats {
    return AdminRoomStats(
        totalCount = roomService.count(),
    )
  }
}
