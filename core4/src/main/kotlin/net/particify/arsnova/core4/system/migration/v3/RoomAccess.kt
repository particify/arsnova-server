/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.time.Instant

data class RoomAccess(
    val roomId: String,
    val userId: String,
    val role: Role,
    val creationTimestamp: Instant,
    val lastAccess: Instant
) {
  enum class Role {
    PARTICIPANT,
    MODERATOR,
    EDITOR,
    OWNER,
  }
}
