/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security

import java.util.UUID
import org.springframework.modulith.NamedInterface

/**
 * Lets the internal token for a room be issued without depending on the module which records room
 * memberships. The returned names are part of the internal token contract, so renaming a role
 * changes the tokens other services receive.
 */
@NamedInterface("permission")
fun interface RoomRoleLookup {
  /**
   * The name of the user's role in the room, as carried in the internal token, or null if the user
   * is not a member.
   */
  fun findRoleName(roomId: UUID, userId: UUID): String?
}
