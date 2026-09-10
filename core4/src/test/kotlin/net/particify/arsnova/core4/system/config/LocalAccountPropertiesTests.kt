/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

private const val PREFIX = "security.local-account"

/**
 * A domain which never matches locks out every registration it was meant to serve and reports the
 * mistake as an error about the submitted address, so the entries are checked while the application
 * starts instead.
 */
class LocalAccountPropertiesTests {
  @Test
  fun shouldBindWithoutDomains() {
    val localAccount = bind(mapOf("enabled" to "true", "self-registration-enabled" to "false"))
    Assertions.assertTrue(localAccount.enabled)
    Assertions.assertFalse(localAccount.selfRegistrationEnabled)
    Assertions.assertEquals(listOf<String>(), localAccount.allowedMailAddressDomains)
  }

  @Test
  fun shouldBindDomains() {
    val localAccount =
        bind(
            mapOf(
                "enabled" to "true",
                "self-registration-enabled" to "true",
                "allowed-mail-address-domains[0]" to "example.com",
                "allowed-mail-address-domains[1]" to "*.example.org"))
    Assertions.assertEquals(
        listOf("example.com", "*.example.org"), localAccount.allowedMailAddressDomains)
  }

  @Test
  fun shouldFailBindingForInvalidDomain() {
    val exception = assertThrows<Exception> { bind(domains("exa mple.com")) }
    Assertions.assertTrue(rootCauseMessage(exception).contains("exa mple.com"))
  }

  @Test
  fun shouldRejectInvalidDomain() {
    val message = rejectionMessage("under_score.example.com")
    Assertions.assertTrue(message.contains("under_score.example.com"))
    Assertions.assertTrue(message.contains("not a valid mail address domain"))
  }

  @Test
  fun shouldRejectEmptyDomain() {
    Assertions.assertTrue(rejectionMessage("").contains("not a valid mail address domain"))
  }

  /** Allowing every domain is what omitting the property does, so `*` would be a second way. */
  @Test
  fun shouldRejectWildcardDomain() {
    val message = rejectionMessage("*")
    Assertions.assertTrue(message.contains("consists of wildcards only"))
    Assertions.assertTrue(message.contains("allowed-mail-address-domains"))
  }

  @Test
  fun shouldRejectDomainOfWildcardsOnly() {
    Assertions.assertTrue(rejectionMessage("*.*").contains("consists of wildcards only"))
  }

  private fun rejectionMessage(domain: String): String =
      assertThrows<IllegalArgumentException> {
            localAccount(allowedMailAddressDomains = listOf(domain))
          }
          .message!!

  private fun rootCauseMessage(exception: Throwable): String =
      generateSequence(exception) { it.cause }.last().message ?: ""

  private fun domains(vararg domains: String) =
      mapOf("enabled" to "true", "self-registration-enabled" to "true")
          .plus(domains.mapIndexed { i, d -> "allowed-mail-address-domains[$i]" to d })

  private fun bind(properties: Map<String, String>): SecurityProperties.LocalAccount {
    val source = MapConfigurationPropertySource(properties.mapKeys { "$PREFIX.${it.key}" })
    return Binder(source).bind(PREFIX, SecurityProperties.LocalAccount::class.java).get()
  }
}
