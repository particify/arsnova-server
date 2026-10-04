/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.net.URI
import java.util.UUID
import net.particify.arsnova.core4.user.internal.LdapProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * The guard is checked on construction, so it fails the application context the same way. It is
 * exercised directly instead of by starting a context with `persistence.v3-migration.enabled`,
 * which would also activate the v3 migration itself and could fail for unrelated reasons.
 */
class LdapRegistrationValidatorTests {
  @Test
  fun shouldRejectMultipleRegistrationsWhileV3MigrationIsEnabled() {
    val exception =
        assertThrows<IllegalArgumentException> {
          createValidator(registrationCount = 2, migrationEnabled = true)
        }
    Assertions.assertTrue(exception.message!!.contains("LDAP registration"))
  }

  @Test
  fun shouldAcceptSingleRegistrationWhileV3MigrationIsEnabled() {
    assertDoesNotThrow { createValidator(registrationCount = 1, migrationEnabled = true) }
  }

  @Test
  fun shouldAcceptMultipleRegistrationsWhileV3MigrationIsDisabled() {
    assertDoesNotThrow { createValidator(registrationCount = 2, migrationEnabled = false) }
  }

  private fun createValidator(
      registrationCount: Int,
      migrationEnabled: Boolean
  ): LdapRegistrationValidator {
    val registrations = (0..<registrationCount).associate { providerId(it) to registration(it) }
    return LdapRegistrationValidator(
        migrationProperties(migrationEnabled), LdapProperties(registrations))
  }

  private fun providerId(index: Int): UUID =
      UUID.fromString("e2b1c33e-1d69-4b0b-9a08-25cbf6b8b8b$index")

  private fun registration(index: Int) =
      LdapProperties.Registration(
          url = "ldap://ldap$index.example.com/dc=example,dc=com",
          userDnPattern = "uid={0},ou=people")

  private fun migrationProperties(enabled: Boolean) =
      MigrationProperties(
          enabled = enabled,
          couchdb =
              MigrationProperties.Couchdb(
                  url = URI("http://couchdb:5984/arsnova3"), username = "u", password = "p"),
          roomAccessUrl = URI("http://authz:8080/roomaccess"))
}
