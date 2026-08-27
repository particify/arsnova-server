/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.UsernameMapping
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

private const val PREFIX = "security.saml2.relyingparty"
private val REGISTRATION_ID = UUID.fromString("fc62cd95-bea9-4aa3-987d-fa976ef300f3")

class Saml2RelyingPartyPropertiesTests {
  @Test
  fun shouldDefaultUsernameMappingToMailAddress() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertEquals(UsernameMapping.MAIL_ADDRESS, registration.usernameMapping)
  }

  /**
   * The inherited base class is bound as a JavaBean, for which a getter-only property fails to bind
   * to anything but its current value.
   */
  @Test
  fun shouldBindConfiguredUsernameMapping() {
    val registration = bind(mapOf("username-mapping" to "ID"))
    Assertions.assertEquals(UsernameMapping.ID, registration.usernameMapping)
  }

  @Test
  fun shouldBindAttributeMappingWithoutLosingDefaults() {
    val registration = bind(mapOf("attribute-mapping.mail-address" to "urn:oid:1.2.3"))
    Assertions.assertEquals("urn:oid:1.2.3", registration.attributeMapping.mailAddress)
    Assertions.assertEquals(
        "urn:oasis:names:tc:SAML:attribute:subject-id", registration.attributeMapping.id)
  }

  private fun bind(
      properties: Map<String, String>
  ): ExtendedSaml2RelyingPartyProperties.ExtendedRegistration {
    val source =
        MapConfigurationPropertySource(
            properties.mapKeys { "$PREFIX.registration.$REGISTRATION_ID.${it.key}" })
    val bound = Binder(source).bind(PREFIX, ExtendedSaml2RelyingPartyProperties::class.java).get()
    return bound.registration.getValue(REGISTRATION_ID)
  }
}
