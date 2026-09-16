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
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding

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
  fun shouldDefaultTitleAndOrder() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertEquals("SAML", registration.title)
    Assertions.assertEquals(0, registration.order)
  }

  /** Fails for a getter-only `title` or `order`, which binds to nothing but its default. */
  @Test
  fun shouldBindConfiguredTitleAndOrder() {
    val registration = bind(mapOf("title" to "Example IdP", "order" to "3"))
    Assertions.assertEquals("Example IdP", registration.title)
    Assertions.assertEquals(3, registration.order)
  }

  @Test
  fun shouldBindAttributeMappingWithoutLosingDefaults() {
    val registration = bind(mapOf("attribute-mapping.mail-address" to "urn:oid:1.2.3"))
    Assertions.assertEquals("urn:oid:1.2.3", registration.attributeMapping.mailAddress)
    Assertions.assertEquals(
        "urn:oasis:names:tc:SAML:attribute:subject-id", registration.attributeMapping.id)
  }

  @Test
  fun shouldDefaultMetadataVerificationToNoCredentials() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertTrue(registration.metadataVerification.credentials.isEmpty())
  }

  /**
   * The list is what carries a federation's key rotation, so the order and the count both have to
   * survive binding. It is bound into a getter-only collection, which only works while the getter
   * hands back the mutable list itself.
   */
  @Test
  fun shouldBindMetadataVerificationCredentials() {
    val registration =
        bind(
            mapOf(
                "metadata-verification.credentials[0].certificate-location" to "file:outgoing.crt",
                "metadata-verification.credentials[1].certificate-location" to "file:incoming.crt"))
    val locations =
        registration.metadataVerification.credentials.map { it.certificateLocation?.filename }
    Assertions.assertEquals(listOf("outgoing.crt", "incoming.crt"), locations)
  }

  /**
   * Boot seeds both, so the repository can apply them to a registration builder unconditionally:
   * the values seeded here are that builder's own defaults.
   */
  @Test
  fun shouldDefaultAssertionConsumerServiceToSpringsEndpoint() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertEquals("{baseUrl}/login/saml2/sso/{registrationId}", registration.acs.location)
    Assertions.assertEquals(Saml2MessageBinding.POST, registration.acs.binding)
  }

  /**
   * What lets a deployment keep publishing the endpoint its identity provider was configured with
   * years ago. Inherited from Boot and reached through a getter-only `getAcs()`, but the leaves
   * themselves carry setters, so neither hits the trap above -- asserted rather than assumed.
   */
  @Test
  fun shouldBindConfiguredAssertionConsumerService() {
    val registration =
        bind(mapOf("acs.location" to "{baseUrl}/auth/callback/saml", "acs.binding" to "redirect"))
    Assertions.assertEquals("{baseUrl}/auth/callback/saml", registration.acs.location)
    Assertions.assertEquals(Saml2MessageBinding.REDIRECT, registration.acs.binding)
  }

  @Test
  fun shouldDefaultDecryptionToNoCredentials() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertTrue(registration.decryption.credentials.isEmpty())
  }

  @Test
  fun shouldBindDecryptionCredentials() {
    val registration =
        bind(
            mapOf(
                "decryption.credentials[0].private-key-location" to "file:decryption.key",
                "decryption.credentials[0].certificate-location" to "file:decryption.crt"))
    val credential = registration.decryption.credentials.single()
    Assertions.assertEquals("decryption.key", credential.privateKeyLocation?.filename)
    Assertions.assertEquals("decryption.crt", credential.certificateLocation?.filename)
  }

  /**
   * Three-valued on purpose: the repository signs unless the property says `false`, so unset and
   * `true` have to stay distinguishable from it after binding.
   */
  @Test
  fun shouldDefaultSignRequestToUnset() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertNull(registration.assertingparty.singlesignon.signRequest)
  }

  @Test
  fun shouldBindConfiguredSignRequest() {
    val registration = bind(mapOf("assertingparty.singlesignon.sign-request" to "true"))
    Assertions.assertEquals(true, registration.assertingparty.singlesignon.signRequest)
  }

  /** The opt-out, which has to reach the binder as `false` rather than as the unset default. */
  @Test
  fun shouldBindSignRequestOptOut() {
    val registration = bind(mapOf("assertingparty.singlesignon.sign-request" to "false"))
    Assertions.assertEquals(false, registration.assertingparty.singlesignon.signRequest)
  }

  @Test
  fun shouldDefaultNameIdFormatToUnset() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertNull(registration.nameIdFormat)
  }

  @Test
  fun shouldBindConfiguredNameIdFormat() {
    val registration =
        bind(mapOf("name-id-format" to "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"))
    Assertions.assertEquals(
        "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", registration.nameIdFormat)
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
