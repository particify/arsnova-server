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

private const val PRODUCT_NAME = "arsnova"

private const val CUSTOM_PATH = "/auth/config/saml/sp-metadata.xml"
private const val OTHER_PATH = "/saml/sp-metadata.xml"

private val REGISTRATION_ID = UUID.fromString("35c3a3c1-6a8e-4c0f-9d3a-1f5b2e8d4c70")
private val OTHER_REGISTRATION_ID = UUID.fromString("7f0e9c24-8b1d-4a5e-9c36-2d7a4e1b8f90")

/** Which registrations publish their metadata at a path of their own, and which are refused. */
class Saml2MetadataPathTests {
  @Test
  fun shouldServeNoExtraPathForUnconfiguredRegistration() {
    Assertions.assertTrue(
        Saml2SpMetadataFactory(PRODUCT_NAME, properties()).configuredMetadataPaths().isEmpty())
  }

  @Test
  fun shouldServeConfiguredPath() {
    Assertions.assertEquals(
        mapOf(REGISTRATION_ID to CUSTOM_PATH),
        Saml2SpMetadataFactory(PRODUCT_NAME, properties(CUSTOM_PATH)).configuredMetadataPaths())
  }

  /** A path of its own per registration, which is the point of it being a registration property. */
  @Test
  fun shouldServeEveryConfiguredPath() {
    Assertions.assertEquals(
        mapOf(REGISTRATION_ID to CUSTOM_PATH, OTHER_REGISTRATION_ID to OTHER_PATH),
        Saml2SpMetadataFactory(PRODUCT_NAME, properties(CUSTOM_PATH, OTHER_PATH))
            .configuredMetadataPaths())
  }

  /** The container prepends the context path, so a configured one would be served twice over. */
  @Test
  fun shouldRejectPathWhichIsNotAbsolute() {
    val exception =
        assertThrows<IllegalArgumentException> {
          Saml2SpMetadataFactory(PRODUCT_NAME, properties("https://example.com/api/saml/metadata"))
              .configuredMetadataPaths()
        }
    Assertions.assertTrue(
        exception.message!!.contains(REGISTRATION_ID.toString()), exception.message)
    Assertions.assertTrue(exception.message!!.contains("metadata-path"), exception.message)
  }

  /** The path carries no registration ID, so a shared one cannot say which document to serve. */
  @Test
  fun shouldRejectSharedPath() {
    val exception =
        assertThrows<IllegalArgumentException> {
          Saml2SpMetadataFactory(PRODUCT_NAME, properties(CUSTOM_PATH, CUSTOM_PATH))
              .configuredMetadataPaths()
        }
    for (expected in listOf(REGISTRATION_ID, OTHER_REGISTRATION_ID, CUSTOM_PATH)) {
      Assertions.assertTrue(exception.message!!.contains("$expected"), exception.message)
    }
  }

  private fun properties(vararg paths: String): ExtendedSaml2RelyingPartyProperties {
    val registrationIds = listOf(REGISTRATION_ID, OTHER_REGISTRATION_ID)
    val registrations =
        registrationIds.take(maxOf(paths.size, 1)).mapIndexed { index, registrationId ->
          registrationId to
              ExtendedRegistration().apply {
                entityId = "https://example.com/saml/$registrationId"
                metadataPath = paths.getOrNull(index)
              }
        }
    return ExtendedSaml2RelyingPartyProperties(registrations.toMap())
  }
}
