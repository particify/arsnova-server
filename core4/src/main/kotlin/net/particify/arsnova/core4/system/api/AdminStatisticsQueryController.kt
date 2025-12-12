/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.api

import net.particify.arsnova.core4.announcement.internal.AnnouncementServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.system.AdminAnnouncementStats
import net.particify.arsnova.core4.system.AdminRoomStats
import net.particify.arsnova.core4.system.AdminStats
import net.particify.arsnova.core4.system.AdminUserStats
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class AdminStatisticsQueryController(
    private val userService: UserServiceImpl,
    private val roomService: RoomServiceImpl,
    private val announcementService: AnnouncementServiceImpl,
) {

  @QueryMapping
  fun adminStats(): AdminStats {
    val userStats =
        AdminUserStats(
            totalCount = userService.count(),
            verified = userService.countByUsernameIsNotNull(),
            pending = userService.countByUsernameIsNullAndUnverifiedMailAddressIsNotNull())
    val roomStats =
        AdminRoomStats(
            totalCount = roomService.count(),
        )
    val announcementStats =
        AdminAnnouncementStats(
            totalCount = announcementService.count(),
        )

    return AdminStats(users = userStats, rooms = roomStats, announcements = announcementStats)
  }
}
