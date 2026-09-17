/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration

private const val PRODUCT_NAME = "arsnova"

private const val CUSTOM_LOCATION = "{baseUrl}/auth/callback/saml"

private val REGISTRATION_ID = UUID.fromString("35c3a3c1-6a8e-4c0f-9d3a-1f5b2e8d4c70")
private val OTHER_REGISTRATION_ID = UUID.fromString("7f0e9c24-8b1d-4a5e-9c36-2d7a4e1b8f90")

private const val MAIL_ATTRIBUTE = "urn:oid:0.9.2342.19200300.100.1.3"
private const val GIVEN_NAME_ATTRIBUTE = "urn:oid:2.5.4.42"
private const val SURNAME_ATTRIBUTE = "urn:oid:2.5.4.4"
private const val EPPN_ATTRIBUTE = "urn:oid:1.3.6.1.4.1.5923.1.1.1.6"
private const val ENTITLEMENT_ATTRIBUTE = "urn:oid:1.3.6.1.4.1.5923.1.1.1.7"
private const val SUBJECT_ID_REQUIREMENT = "urn:oasis:names:tc:SAML:profiles:subject-id:req"
private const val URI_NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri"

private val REQUESTED_ATTRIBUTE_PATTERN = Regex("<[\\w:]*RequestedAttribute\\s[^>]*>")
private val XML_ATTRIBUTE_PATTERN = Regex("([\\w:]+)=\"([^\"]*)\"")

/**
 * What the published service provider metadata asks an identity provider to release. Asserted on
 * the serialized document, because that is what a federation validator reads.
 */
class Saml2SpMetadataTests {
  /** Guards the reason the customizer exists: if Spring starts emitting this, it duplicates it. */
  @Test
  fun shouldNotDeclareAttributeConsumingServiceWithoutCustomizer() {
    val metadata = OpenSaml5MetadataResolver().resolve(registration())
    Assertions.assertFalse(metadata.contains("AttributeConsumingService"), metadata)
  }

  /** Spring builds the name ID format from the registration, and this one configures none. */
  @Test
  fun shouldDeclareRequestedAttributesWithoutNameIdFormat() {
    val metadata = resolve()
    Assertions.assertTrue(metadata.contains("AttributeConsumingService"), metadata)
    Assertions.assertFalse(metadata.contains("NameIDFormat"), metadata)
  }

  /**
   * The declaration follows the registration rather than being stated outright, because an identity
   * provider rejects a request which is not signed the way the document says it is.
   */
  @Test
  fun shouldDeclareAuthnRequestsSignedForSigningRegistration() {
    val metadata = resolve(signed = true)
    Assertions.assertTrue(metadata.contains("AuthnRequestsSigned=\"true\""), metadata)
  }

  /** An absent attribute already means `false`, so opting out declares nothing. */
  @Test
  fun shouldNotDeclareAuthnRequestsSignedForRegistrationWhichOptedOut() {
    val metadata = resolve()
    Assertions.assertFalse(metadata.contains("AuthnRequestsSigned"), metadata)
  }

  /** Guards the reason the customizer exists: if Spring starts emitting this, it duplicates it. */
  @Test
  fun shouldNotDeclareAuthnRequestsSignedWithoutCustomizer() {
    val metadata = OpenSaml5MetadataResolver().resolve(registration(signed = true))
    Assertions.assertFalse(metadata.contains("AuthnRequestsSigned"), metadata)
  }

  @Test
  fun shouldPublishProductNameAsServiceName() {
    val metadata = resolve()
    Assertions.assertTrue(metadata.contains(">$PRODUCT_NAME<"), metadata)
    Assertions.assertTrue(metadata.contains("xml:lang=\"en\""), metadata)
    Assertions.assertTrue(metadata.contains("index=\"0\""), metadata)
    Assertions.assertTrue(metadata.contains("isDefault=\"true\""), metadata)
  }

  /**
   * The design relies on `SPSSODescriptorImpl.getOrderedChildren()` putting a post-hoc addition
   * where the schema wants it; the wrong order yields metadata a federation validator rejects and
   * nothing else would catch it.
   */
  @Test
  fun shouldDeclareAttributeConsumingServiceAfterAssertionConsumerService() {
    val metadata = resolve()
    Assertions.assertTrue(
        metadata.indexOf("AttributeConsumingService") >
            metadata.indexOf("AssertionConsumerService"),
        metadata)
  }

  /**
   * A subject identifier is not released through attribute filtering, so asking for it as a
   * requested attribute would be inert. The entity attribute is the channel federations implement.
   */
  @Test
  fun shouldRequestSubjectIdThroughEntityAttribute() {
    val metadata = resolve()
    Assertions.assertTrue(metadata.contains(SUBJECT_ID_REQUIREMENT), metadata)
    Assertions.assertTrue(metadata.contains(">subject-id<"), metadata)
    Assertions.assertEquals(
        listOf(MAIL_ATTRIBUTE, GIVEN_NAME_ATTRIBUTE, SURNAME_ATTRIBUTE), requestedNames(metadata))
    Assertions.assertTrue(requestedAttributes(metadata).none { it["isRequired"] == "true" })
  }

  @Test
  fun shouldRequestPairwiseIdThroughEntityAttribute() {
    val metadata = resolve(id = "urn:oasis:names:tc:SAML:attribute:pairwise-id")
    Assertions.assertTrue(metadata.contains(">pairwise-id<"), metadata)
    Assertions.assertFalse(requestedNames(metadata).contains("pairwise-id"), metadata)
  }

  /** An ordinary attribute has to be requested to be released, and the login fails without it. */
  @Test
  fun shouldRequireIdMappedToOrdinaryAttribute() {
    val metadata = resolve(id = EPPN_ATTRIBUTE)
    Assertions.assertFalse(metadata.contains(SUBJECT_ID_REQUIREMENT), metadata)
    val attribute = requestedAttributes(metadata).single { it["Name"] == EPPN_ATTRIBUTE }
    Assertions.assertEquals("true", attribute["isRequired"])
    Assertions.assertEquals(URI_NAME_FORMAT, attribute["NameFormat"])
  }

  /** An absent `NameFormat` already means `unspecified`, so a short name needs none stated. */
  @Test
  fun shouldOmitNameFormatForShortAttributeName() {
    val metadata = resolve(id = "uid")
    val attribute = requestedAttributes(metadata).single { it["Name"] == "uid" }
    Assertions.assertNull(attribute["NameFormat"])
  }

  /** Two mappings may point at one attribute, which must not be requested twice. */
  @Test
  fun shouldRequestAttributeMappedTwiceOnlyOnce() {
    val metadata = resolve(id = MAIL_ATTRIBUTE)
    val attributes = requestedAttributes(metadata).filter { it["Name"] == MAIL_ATTRIBUTE }
    Assertions.assertEquals(1, attributes.size, metadata)
    Assertions.assertEquals("true", attributes.single()["isRequired"])
  }

  @Test
  fun shouldAppendAdditionalRequestedAttributes() {
    val metadata =
        resolve(additional = listOf(EPPN_ATTRIBUTE to true, ENTITLEMENT_ATTRIBUTE to false))
    Assertions.assertEquals(
        listOf(
            MAIL_ATTRIBUTE,
            GIVEN_NAME_ATTRIBUTE,
            SURNAME_ATTRIBUTE,
            EPPN_ATTRIBUTE,
            ENTITLEMENT_ATTRIBUTE),
        requestedNames(metadata))
    val attributes = requestedAttributes(metadata).associateBy { it["Name"] }
    Assertions.assertEquals("true", attributes[EPPN_ATTRIBUTE]!!["isRequired"])
    Assertions.assertEquals("false", attributes[ENTITLEMENT_ATTRIBUTE]!!["isRequired"])
  }

  /** A derived attribute keeps its own requirement, whatever the extra repeating it asks for. */
  @Test
  fun shouldNotRepeatDerivedAttributeAskedForAgain() {
    val metadata = resolve(id = EPPN_ATTRIBUTE, additional = listOf(EPPN_ATTRIBUTE to false))
    val attributes = requestedAttributes(metadata).filter { it["Name"] == EPPN_ATTRIBUTE }
    Assertions.assertEquals(1, attributes.size, metadata)
    Assertions.assertEquals("true", attributes.single()["isRequired"])
  }

  /**
   * The aggregate document covers every registration, so one unresolvable ID may not take the rest
   * of them down with it.
   */
  @Test
  fun shouldServeMetadataForUnconfiguredRegistration() {
    val resolver = Saml2SpMetadataFactory(PRODUCT_NAME, properties()).metadataResolver()
    val metadata = resolver.resolve(registration(OTHER_REGISTRATION_ID))
    Assertions.assertTrue(metadata.contains("EntityDescriptor"), metadata)
    Assertions.assertFalse(metadata.contains("AttributeConsumingService"), metadata)
  }

  private fun resolve(
      id: String? = null,
      additional: List<Pair<String, Boolean>> = listOf(),
      signed: Boolean = false
  ): String {
    val properties = properties()
    val registration = properties.registration.getValue(REGISTRATION_ID)
    id?.let { registration.attributeMapping.id = it }
    additional.mapTo(registration.additionalRequestedAttributes) { (name, required) ->
      ExtendedRegistration.RequestedAttribute().apply {
        this.name = name
        this.required = required
      }
    }
    return Saml2SpMetadataFactory(PRODUCT_NAME, properties)
        .metadataResolver()
        .resolve(registration(signed = signed))
  }

  private fun requestedAttributes(metadata: String): List<Map<String, String>> =
      REQUESTED_ATTRIBUTE_PATTERN.findAll(metadata)
          .map { element ->
            XML_ATTRIBUTE_PATTERN.findAll(element.value).associate {
              it.groupValues[1] to it.groupValues[2]
            }
          }
          .toList()

  private fun requestedNames(metadata: String): List<String> =
      requestedAttributes(metadata).mapNotNull { it["Name"] }

  private fun properties(): ExtendedSaml2RelyingPartyProperties {
    val registration =
        ExtendedRegistration().apply { entityId = "https://example.com/saml/$REGISTRATION_ID" }
    return ExtendedSaml2RelyingPartyProperties(mapOf(REGISTRATION_ID to registration))
  }

  private fun registration(
      registrationId: UUID = REGISTRATION_ID,
      signed: Boolean = false
  ): RelyingPartyRegistration =
      RelyingPartyRegistration.withRegistrationId(registrationId.toString())
          .entityId("https://example.com/saml/$registrationId")
          .assertionConsumerServiceLocation(CUSTOM_LOCATION)
          .authnRequestsSigned(signed)
          .assertingPartyMetadata {
            it.entityId("https://idp.example.com/saml")
                .singleSignOnServiceLocation("https://idp.example.com/saml/sso")
          }
          .build()
}
