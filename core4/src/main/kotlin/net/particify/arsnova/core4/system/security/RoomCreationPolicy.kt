/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.SecurityProperties.RoomCreatorRole.AutoAssignment
import net.particify.arsnova.core4.user.User
import org.springframework.stereotype.Component

/**
 * Decides which accounts may create rooms. The authority granted per request and the policy
 * published to clients are both derived from it, so the two cannot drift apart.
 *
 * Administrators are not exempt: they are subject to the same setting as any other account.
 */
@Component
class RoomCreationPolicy(private val securityProperties: SecurityProperties) {
  fun mayCreateRooms(user: User): Boolean =
      if (user.username != null) mayVerifiedAccountsCreateRooms()
      else mayUnverifiedAccountsCreateRooms()

  fun mayVerifiedAccountsCreateRooms(): Boolean = autoAssignTo() != AutoAssignment.NONE

  fun mayUnverifiedAccountsCreateRooms(): Boolean = autoAssignTo() == AutoAssignment.ALL_ACCOUNTS

  private fun autoAssignTo(): AutoAssignment = securityProperties.roomCreatorRole.autoAssignTo
}
