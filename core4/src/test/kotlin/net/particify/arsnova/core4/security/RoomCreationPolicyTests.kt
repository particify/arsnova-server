/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security

import java.util.UUID
import net.particify.arsnova.core4.security.SecurityProperties.RoomCreatorRole.AutoAssignment
import net.particify.arsnova.core4.user.AuthenticatedUser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RoomCreationPolicyTests {
  private val verifiedUser = AuthenticatedUser(UUID.randomUUID(), setOf("USER"), true)
  private val unverifiedUser = AuthenticatedUser(UUID.randomUUID(), setOf("USER"), false)
  private val adminUser = AuthenticatedUser(UUID.randomUUID(), setOf("ADMIN", "USER"), true)

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
