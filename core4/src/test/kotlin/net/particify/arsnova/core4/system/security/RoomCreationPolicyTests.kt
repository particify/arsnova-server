/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.SecurityProperties.RoomCreatorRole.AutoAssignment
import net.particify.arsnova.core4.system.config.securityProperties
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RoomCreationPolicyTests {
  private val verifiedUser = User(username = "user@example.com")
  private val unverifiedUser = User()
  private val adminUser =
      User(
          username = "admin@example.com",
          roles = mutableSetOf(Role().apply { name = "ADMIN" }, Role().apply { name = "USER" }))

  @Test
  fun shouldAllowEveryAccountForAllAccounts() {
    val policy = policy(AutoAssignment.ALL_ACCOUNTS)
    Assertions.assertTrue(policy.mayCreateRooms(verifiedUser))
    Assertions.assertTrue(policy.mayCreateRooms(unverifiedUser))
    Assertions.assertTrue(policy.mayVerifiedAccountsCreateRooms())
    Assertions.assertTrue(policy.mayUnverifiedAccountsCreateRooms())
  }

  @Test
  fun shouldAllowOnlyVerifiedAccountsForVerifiedAccounts() {
    val policy = policy(AutoAssignment.VERIFIED_ACCOUNTS)
    Assertions.assertTrue(policy.mayCreateRooms(verifiedUser))
    Assertions.assertFalse(policy.mayCreateRooms(unverifiedUser))
    Assertions.assertTrue(policy.mayVerifiedAccountsCreateRooms())
    Assertions.assertFalse(policy.mayUnverifiedAccountsCreateRooms())
  }

  @Test
  fun shouldAllowNoAccountForNone() {
    val policy = policy(AutoAssignment.NONE)
    Assertions.assertFalse(policy.mayCreateRooms(verifiedUser))
    Assertions.assertFalse(policy.mayCreateRooms(unverifiedUser))
    Assertions.assertFalse(policy.mayVerifiedAccountsCreateRooms())
    Assertions.assertFalse(policy.mayUnverifiedAccountsCreateRooms())
  }

  /** Administrators are deliberately not exempt from the setting. */
  @Test
  fun shouldDenyAdministratorForNone() {
    Assertions.assertTrue(policy(AutoAssignment.ALL_ACCOUNTS).mayCreateRooms(adminUser))
    Assertions.assertFalse(policy(AutoAssignment.NONE).mayCreateRooms(adminUser))
  }

  private fun policy(autoAssignTo: AutoAssignment) =
      RoomCreationPolicy(
          securityProperties(roomCreatorRole = SecurityProperties.RoomCreatorRole(autoAssignTo)))
}
