/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system

data class AdminStats(
    val users: AdminUserStats,
    val rooms: AdminRoomStats,
    val announcements: AdminAnnouncementStats
)

data class AdminUserStats(
    val totalCount: Long,
    val verified: Long = 0,
    val pending: Long = 0,
    val deleted: Long = 0
)

data class AdminRoomStats(val totalCount: Long, val deleted: Long = 0)

data class AdminAnnouncementStats(
    val totalCount: Long,
    val deleted: Long = 0,
)
