/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

data class AdminActiveRoomStats(
    val count: Long,
    val minMemberCount: Int,
    val activityWindowMinutes: Int
)
