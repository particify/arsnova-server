/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.UUID
import net.particify.arsnova.core4.room.RoomRole

data class RoomMembershipQueryInput(
    val room: CreateRoomInput?,
    val userId: UUID?,
    val role: RoomRole?
)
