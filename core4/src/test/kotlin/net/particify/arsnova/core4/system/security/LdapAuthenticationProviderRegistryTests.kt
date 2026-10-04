/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.LdapProperties
import net.particify.arsnova.core4.user.internal.LdapUserDetailsContextMapperFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LdapAuthenticationProviderRegistryTests {
  @Autowired lateinit var mapperFactory: LdapUserDetailsContextMapperFactory
  @Autowired lateinit var eventPublisher: ApplicationEventPublisher

  @Test
  fun shouldKeepOneAuthenticationManagerPerRegistration() {
    val registry = createRegistry(registrationCount = 2)
    Assertions.assertNotNull(registry.findByProviderId(providerId(0)))
    Assertions.assertNotNull(registry.findByProviderId(providerId(1)))
    Assertions.assertNull(registry.findByProviderId(UUID.randomUUID()))
  }

  private fun createRegistry(registrationCount: Int): LdapAuthenticationProviderRegistry {
    val registrations = (0..<registrationCount).associate { providerId(it) to registration(it) }
    return LdapAuthenticationProviderRegistry(
        LdapProperties(registrations), mapperFactory, eventPublisher)
  }

  private fun providerId(index: Int): UUID =
      UUID.fromString("e2b1c33e-1d69-4b0b-9a08-25cbf6b8b8b$index")

  private fun registration(index: Int) =
      LdapProperties.Registration(
          url = "ldap://ldap$index.example.com/dc=example,dc=com",
          userDnPattern = "uid={0},ou=people")
}
