/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val CUSTOM_LOCATION = "{baseUrl}/auth/callback/saml"
private const val CUSTOM_PATH = "/auth/callback/saml"
private const val OTHER_LOCATION = "{baseUrl}/saml/acs"
private const val OTHER_PATH = "/saml/acs"

private val REGISTRATION_ID = UUID.fromString("35c3a3c1-6a8e-4c0f-9d3a-1f5b2e8d4c70")
private val OTHER_REGISTRATION_ID = UUID.fromString("7f0e9c24-8b1d-4a5e-9c36-2d7a4e1b8f90")

/** Which paths get a filter of their own, and which configurations are refused outright. */
class Saml2AssertionConsumerServicePathTests {
  @Test
  fun shouldNotServeAnyPathForUnconfiguredRegistration() {
    Assertions.assertTrue(Saml2AssertionConsumerServices(properties()).configuredPaths().isEmpty())
  }

  @Test
  fun shouldDerivePathWithoutBaseUrlPlaceholder() {
    val paths = Saml2AssertionConsumerServices(properties(CUSTOM_LOCATION)).configuredPaths()
    Assertions.assertEquals(mapOf(REGISTRATION_ID to CUSTOM_PATH), paths)
  }

  /** The context path lives inside `{baseUrl}`, so an absolute location cannot be split up. */
  @Test
  fun shouldRejectLocationWithoutBaseUrlPlaceholder() {
    val properties = properties("https://example.com/api/auth/callback/saml")
    val exception =
        assertThrows<IllegalArgumentException> {
          Saml2AssertionConsumerServices(properties).configuredPaths()
        }
    Assertions.assertTrue(
        exception.message!!.contains(REGISTRATION_ID.toString()), exception.message)
  }

  /**
   * An ordinary Spring Boot setting rather than a mode, so any number of registrations may use it.
   */
  @Test
  fun shouldServeEveryCustomAssertionConsumerServicePath() {
    val paths =
        Saml2AssertionConsumerServices(properties(CUSTOM_LOCATION, OTHER_LOCATION))
            .configuredPaths()
    Assertions.assertEquals(
        mapOf(REGISTRATION_ID to CUSTOM_PATH, OTHER_REGISTRATION_ID to OTHER_PATH), paths)
  }

  /**
   * An assertion consumer service carries no registration ID, so a shared path resolves nothing.
   */
  @Test
  fun shouldRejectSharedAssertionConsumerServicePath() {
    val properties = properties(CUSTOM_LOCATION, CUSTOM_LOCATION)
    val exception =
        assertThrows<IllegalArgumentException> {
          Saml2AssertionConsumerServices(properties).configuredPaths()
        }
    for (registrationId in listOf(REGISTRATION_ID, OTHER_REGISTRATION_ID, CUSTOM_PATH)) {
      Assertions.assertTrue(exception.message!!.contains("$registrationId"), exception.message)
    }
  }

  private fun properties(vararg locations: String): ExtendedSaml2RelyingPartyProperties {
    val registrationIds = listOf(REGISTRATION_ID, OTHER_REGISTRATION_ID)
    val registrations =
        registrationIds.take(maxOf(locations.size, 1)).mapIndexed { index, registrationId ->
          registrationId to
              ExtendedRegistration().apply {
                entityId = "https://example.com/saml/$registrationId"
                locations.getOrNull(index)?.let { acs.location = it }
              }
        }
    return ExtendedSaml2RelyingPartyProperties(registrations.toMap())
  }
}
