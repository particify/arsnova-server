/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.net.URI
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.migration.v3.MigrationProperties
import net.particify.arsnova.core4.user.internal.LdapProperties
import net.particify.arsnova.core4.user.internal.LdapUserDetailsContextMapperFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * The guard is checked on construction, so it fails the application context the same way. It is
 * exercised directly instead of by starting a context with `persistence.v3-migration.enabled`,
 * which would also activate the v3 migration itself and could fail for unrelated reasons.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LdapAuthenticationProviderRegistryTests {
  @Autowired lateinit var mapperFactory: LdapUserDetailsContextMapperFactory
  @Autowired lateinit var eventPublisher: ApplicationEventPublisher

  @Test
  fun shouldRejectMultipleRegistrationsWhileV3MigrationIsEnabled() {
    val exception =
        assertThrows<IllegalArgumentException> {
          createRegistry(registrationCount = 2, migrationEnabled = true)
        }
    Assertions.assertTrue(exception.message!!.contains("LDAP registration"))
  }

  @Test
  fun shouldAcceptSingleRegistrationWhileV3MigrationIsEnabled() {
    assertDoesNotThrow { createRegistry(registrationCount = 1, migrationEnabled = true) }
  }

  @Test
  fun shouldAcceptMultipleRegistrationsWhileV3MigrationIsDisabled() {
    val registry = createRegistry(registrationCount = 2, migrationEnabled = false)
    Assertions.assertNotNull(registry.findByProviderId(providerId(0)))
    Assertions.assertNotNull(registry.findByProviderId(providerId(1)))
    Assertions.assertNull(registry.findByProviderId(UUID.randomUUID()))
  }

  private fun createRegistry(
      registrationCount: Int,
      migrationEnabled: Boolean
  ): LdapAuthenticationProviderRegistry {
    val registrations = (0..<registrationCount).associate { providerId(it) to registration(it) }
    return LdapAuthenticationProviderRegistry(
        LdapProperties(registrations),
        migrationProperties(migrationEnabled),
        mapperFactory,
        eventPublisher)
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
