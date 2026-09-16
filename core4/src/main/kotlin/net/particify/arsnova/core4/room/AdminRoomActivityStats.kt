/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

data class AdminRoomActivityStats(
    val managingUserCount: Long = 0,
    val participantCount: Long = 0,
    val roomCount: Long = 0,
)
