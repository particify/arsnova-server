/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

data class AdminUserStats(
    val totalCount: Long = 0,
    val verifiedCount: Long = 0,
    val pendingCount: Long = 0,
)
