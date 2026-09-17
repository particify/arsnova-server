/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.UsernameMapping
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

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
  fun shouldDefaultAdditionalRequestedAttributesToNone() {
    val registration = bind(mapOf("entity-id" to "https://example.com"))
    Assertions.assertTrue(registration.additionalRequestedAttributes.isEmpty())
  }

  /**
   * Order decides the order the attributes are published in, and `required` is what an identity
   * provider keys its release policy on, so both have to survive binding. Bound into a getter-only
   * collection, which only works while the getter hands back the mutable list itself.
   */
  @Test
  fun shouldBindAdditionalRequestedAttributes() {
    val registration =
        bind(
            mapOf(
                "additional-requested-attributes[0].name" to "urn:oid:1.3.6.1.4.1.5923.1.1.1.9",
                "additional-requested-attributes[0].required" to "true",
                "additional-requested-attributes[1].name" to "urn:oid:2.16.840.1.113730.3.1.241"))
    Assertions.assertEquals(
        listOf("urn:oid:1.3.6.1.4.1.5923.1.1.1.9", "urn:oid:2.16.840.1.113730.3.1.241"),
        registration.additionalRequestedAttributes.map { it.name })
    Assertions.assertEquals(
        listOf(true, false), registration.additionalRequestedAttributes.map { it.required })
  }

  /**
   * Nothing can be requested without a name, and dropping the entry while the document is resolved
   * would hide the typo, so it has to stop the application from starting instead.
   */
  @Test
  fun shouldRejectBlankAdditionalRequestedAttributeName() {
    val exception =
        assertThrows<BindException> {
          bindAndValidate(mapOf("additional-requested-attributes[0].name" to " "))
        }
    val message = messages(exception)
    Assertions.assertTrue(message.contains("additional-requested-attributes[0].name"), message)
    Assertions.assertTrue(message.contains("must name the attribute to request"), message)
  }

  /**
   * Blank is not a way to switch a mapping off: the converter would look the empty name up in the
   * assertion and the metadata would request it, so it has to be caught before either happens.
   */
  @Test
  fun shouldRejectBlankAttributeMappingValue() {
    val exception =
        assertThrows<BindException> { bindAndValidate(mapOf("attribute-mapping.given-name" to "")) }
    val message = messages(exception)
    Assertions.assertTrue(message.contains("attribute-mapping.given-name"), message)
    Assertions.assertTrue(
        message.contains("must name the asserted attribute holding the given name"), message)
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

  /** Everything the exception chain says, which is where the offending property path shows up. */
  private fun messages(exception: Throwable): String =
      generateSequence(exception) { it.cause }.joinToString("\n") { it.message.orEmpty() }

  /**
   * Binds the way Spring Boot does for a `@Validated` target, which a plain [Binder] does not: Boot
   * installs the same [ValidationBindHandler] and fails startup on what it reports.
   */
  private fun bindAndValidate(properties: Map<String, String>) {
    val validator = LocalValidatorFactoryBean()
    validator.afterPropertiesSet()
    Binder(source(properties))
        .bind(
            PREFIX,
            Bindable.of(ExtendedSaml2RelyingPartyProperties::class.java),
            ValidationBindHandler(validator))
  }

  private fun source(properties: Map<String, String>) =
      MapConfigurationPropertySource(
          properties.mapKeys { "$PREFIX.registration.$REGISTRATION_ID.${it.key}" })

  private fun bind(
      properties: Map<String, String>
  ): ExtendedSaml2RelyingPartyProperties.ExtendedRegistration {
    val bound =
        Binder(source(properties))
            .bind(PREFIX, ExtendedSaml2RelyingPartyProperties::class.java)
            .get()
    return bound.registration.getValue(REGISTRATION_ID)
  }
}
