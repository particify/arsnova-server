/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

data class AdminRoomStats(
    val totalCount: Long = 0,
    val membershipCount: Long = 0,
    val activeRoomCount: Long = 0,
    val managingUserCount: Long = 0,
    val participantCount: Long = 0
)
