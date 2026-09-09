/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import net.particify.arsnova.core4.user.ExternalLoginLinkingStrategy
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

private const val STRATEGY_NAME = "test-strategy"
private const val UNKNOWN_STRATEGY_NAME = "no-such-strategy"
private const val LDAP_REGISTRATION_ID = "5f0a2c48-9d31-4c7e-8b52-6a1e3f0d9c74"
private const val SAML_REGISTRATION_ID = "7c3e5a19-2b84-4d60-9f13-8e5a6c2b0d47"

/**
 * The configuration is validated while the registry is constructed, and the registry is a singleton
 * bean, so a registration selecting an unknown strategy fails the application start rather than the
 * first login which would reach it.
 */
class ExternalLoginLinkingStrategyRegistryTests {
  private val strategy = TestLinkingStrategy(STRATEGY_NAME)

  @Test
  fun shouldRejectUnknownStrategySelectedByLdapRegistration() {
    val exception =
        assertThrows<IllegalArgumentException> {
          createRegistry(ldapStrategy = UNKNOWN_STRATEGY_NAME)
        }
    assertMessageNames(exception, LDAP_REGISTRATION_ID)
  }

  @Test
  fun shouldRejectUnknownStrategySelectedBySaml2Registration() {
    val exception =
        assertThrows<IllegalArgumentException> {
          createRegistry(saml2Strategy = UNKNOWN_STRATEGY_NAME)
        }
    assertMessageNames(exception, SAML_REGISTRATION_ID)
  }

  /** What a build without any strategy of its own reports, so the message has to stay useful. */
  @Test
  fun shouldReportThatNoStrategyIsAvailableWhenNoneIsRegistered() {
    val exception =
        assertThrows<IllegalArgumentException> {
          createRegistry(strategies = listOf(), ldapStrategy = UNKNOWN_STRATEGY_NAME)
        }
    Assertions.assertTrue(exception.message!!.contains("Available strategies: none."))
  }

  @Test
  fun shouldRejectStrategiesSharingAName() {
    val duplicate = listOf(strategy, TestLinkingStrategy(STRATEGY_NAME))
    assertThrows<IllegalArgumentException> { createRegistry(strategies = duplicate) }
  }

  @Test
  fun shouldAcceptRegistrationsSelectingARegisteredStrategy() {
    assertDoesNotThrow {
      createRegistry(ldapStrategy = STRATEGY_NAME, saml2Strategy = STRATEGY_NAME)
    }
  }

  @Test
  fun shouldResolveSelectedStrategyByName() {
    val registry = createRegistry()
    Assertions.assertSame(strategy, registry.find(STRATEGY_NAME))
  }

  /** A registration which selects nothing is never linked, whatever is registered. */
  @Test
  fun shouldResolveNoStrategyWithoutASelection() {
    Assertions.assertNull(createRegistry().find(null))
  }

  private fun assertMessageNames(exception: IllegalArgumentException, registrationId: String) {
    val message = exception.message!!
    Assertions.assertTrue(message.contains(registrationId), message)
    Assertions.assertTrue(message.contains(UNKNOWN_STRATEGY_NAME), message)
    Assertions.assertTrue(message.contains(STRATEGY_NAME), message)
  }

  private fun createRegistry(
      strategies: List<ExternalLoginLinkingStrategy> = listOf(strategy),
      ldapStrategy: String? = null,
      saml2Strategy: String? = null
  ) =
      ExternalLoginLinkingStrategyRegistry(
          strategies, ldapProperties(ldapStrategy), saml2Properties(saml2Strategy))

  private fun ldapProperties(strategyName: String?) =
      LdapProperties(
          mapOf(
              UUID.fromString(LDAP_REGISTRATION_ID) to
                  LdapProperties.Registration(
                      url = "ldap://ldap.example.com/dc=example,dc=com",
                      userDnPattern = "uid={0},ou=people",
                      linkingStrategy = strategyName)))

  private fun saml2Properties(strategyName: String?) =
      ExtendedSaml2RelyingPartyProperties(
          mapOf(
              UUID.fromString(SAML_REGISTRATION_ID) to
                  ExtendedRegistration().apply { linkingStrategy = strategyName }))
}

private class TestLinkingStrategy(override val name: String) : ExternalLoginLinkingStrategy {
  override fun findLinkTarget(providerId: UUID, externalId: String, mailAddress: String?): User? =
      null
}
