/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.time.Duration
import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.SecurityProperties.RoomCreatorRole.AutoAssignment
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val SECRET = "weaksecret1234567890123456789012"
private const val BCRYPT_STRENGTH = 12
private const val CHALLENGE_VALIDITY_SECONDS = 60L
private const val CHALLENGE_ITERATIONS = 5000
private const val LOGIN_ATTEMPT_LIMIT = 20L

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
          SecurityProperties(
              password = SecurityProperties.Password(BCRYPT_STRENGTH),
              jwt = SecurityProperties.Jwt(SECRET, "issuer", null, Duration.ofHours(1)),
              challenge =
                  SecurityProperties.Challenge(
                      CHALLENGE_VALIDITY_SECONDS,
                      "PBKDF2/SHA-256",
                      SECRET,
                      CHALLENGE_ITERATIONS,
                      CHALLENGE_ITERATIONS),
              login = SecurityProperties.Login(LOGIN_ATTEMPT_LIMIT, Duration.ofMinutes(2)),
              roomCreatorRole = SecurityProperties.RoomCreatorRole(autoAssignTo),
              authorizeUriHeader = "X-Forwarded-Uri",
              authorizeUriPrefix = "/api"))
}
