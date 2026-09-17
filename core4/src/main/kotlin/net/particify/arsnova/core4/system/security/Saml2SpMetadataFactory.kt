/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.opensaml.core.xml.schema.XSAny
import org.opensaml.core.xml.schema.impl.XSAnyBuilder
import org.opensaml.saml.ext.saml2mdattr.impl.EntityAttributesBuilder
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AttributeValue
import org.opensaml.saml.saml2.core.impl.AttributeBuilder
import org.opensaml.saml.saml2.metadata.AttributeConsumingService
import org.opensaml.saml.saml2.metadata.EntityDescriptor
import org.opensaml.saml.saml2.metadata.RequestedAttribute
import org.opensaml.saml.saml2.metadata.SPSSODescriptor
import org.opensaml.saml.saml2.metadata.ServiceName
import org.opensaml.saml.saml2.metadata.impl.AttributeConsumingServiceBuilder
import org.opensaml.saml.saml2.metadata.impl.ExtensionsBuilder
import org.opensaml.saml.saml2.metadata.impl.RequestedAttributeBuilder
import org.opensaml.saml.saml2.metadata.impl.ServiceNameBuilder
import org.slf4j.LoggerFactory
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver.EntityDescriptorParameters
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResponseResolver
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.metadata.RequestMatcherMetadataResponseResolver

/**
 * Published alongside the requested attributes while the product name is the only display name we
 * have. Hardcoded rather than configurable because no other language is published either.
 */
private const val SERVICE_NAME_LANGUAGE = "en"

/**
 * A subject identifier is released by a dedicated profile rather than by attribute filtering, so
 * asking for it as a [RequestedAttribute] achieves nothing. The requirement is stated through an
 * entity attribute instead, whose value is the bare identifier kind.
 */
private const val SUBJECT_ID_REQUIREMENT_ATTRIBUTE =
    "urn:oasis:names:tc:SAML:profiles:subject-id:req"

private val SUBJECT_ID_REQUIREMENTS =
    mapOf(
        "urn:oasis:names:tc:SAML:attribute:pairwise-id" to "pairwise-id",
        "urn:oasis:names:tc:SAML:attribute:subject-id" to "subject-id")

private val logger = LoggerFactory.getLogger(Saml2SpMetadataFactory::class.java)

/**
 * Builds the service provider metadata document and resolves it for the endpoint it is served at.
 * The product name and the SAML configuration are fixed for the lifetime of the application
 * context, so they are held here rather than handed to each declaration in turn.
 */
class Saml2SpMetadataFactory(
    private val productName: String,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  /** Resolves the document served at `/saml2/service-provider-metadata/{registrationId}`. */
  fun metadataResponseResolver(
      registrations: RelyingPartyRegistrationRepository
  ): Saml2MetadataResponseResolver =
      RequestMatcherMetadataResponseResolver(registrations, metadataResolver())

  /**
   * Emits everything Spring builds itself plus the declarations only [declareServiceProvider]
   * makes. Spring has no declarative equivalent for those -- neither `RelyingPartyRegistration` nor
   * Boot's properties carry an `AttributeConsumingService` -- and the customizer runs before the
   * document is signed, so what it adds is covered by the signature.
   */
  fun metadataResolver(): OpenSaml5MetadataResolver {
    val resolver = OpenSaml5MetadataResolver()
    resolver.setEntityDescriptorCustomizer { declareServiceProvider(it) }
    return resolver
  }

  /**
   * States everything the published document says beyond what Spring derives from a
   * [RelyingPartyRegistration]. The descriptor is resolved once and handed to each declaration, so
   * the role descriptors are walked a single time.
   */
  private fun declareServiceProvider(parameters: EntityDescriptorParameters) {
    val descriptor = serviceProviderDescriptor(parameters)
    declareAuthnRequestsSigned(parameters.relyingPartyRegistration, descriptor)
    declareRequestedAttributes(parameters, descriptor)
  }

  /**
   * Declares what the backend reads from an assertion, so a federated identity provider has
   * something to key its release policy on. The descriptor is passed in rather than resolved here
   * so a customizer which declares more than this walks the role descriptors once.
   */
  private fun declareRequestedAttributes(
      parameters: EntityDescriptorParameters,
      descriptor: SPSSODescriptor
  ) {
    val registration =
        extendedRegistration(parameters.relyingPartyRegistration.registrationId) ?: return
    SUBJECT_ID_REQUIREMENTS[registration.attributeMapping.id]?.let {
      declareSubjectIdRequirement(parameters.entityDescriptor, it)
    }
    descriptor.attributeConsumingServices.add(
        attributeConsumingService(requestedAttributes(registration)))
  }

  /**
   * Soft on purpose: the aggregate document covers every registration at once, so one ID which
   * resolves to no configuration would otherwise take down the metadata of all of them. The login
   * path fails hard on the same lookup, where there is no document left to serve.
   */
  private fun extendedRegistration(registrationId: String): ExtendedRegistration? {
    val registration =
        runCatching { UUID.fromString(registrationId) }
            .getOrNull()
            ?.let { saml2Properties.registration[it] }
    if (registration == null) {
      logger.warn(
          "No SAML configuration found for registration {}. Publishing its metadata without " +
              "requested attributes.",
          registrationId)
    }
    return registration
  }

  /**
   * `index` is required by the schema and `isDefault` is what makes a single service unambiguous,
   * which is why no `AttributeConsumingServiceIndex` is sent with an authentication request.
   */
  private fun attributeConsumingService(
      attributes: Map<String, Boolean>
  ): AttributeConsumingService {
    val service = AttributeConsumingServiceBuilder().buildObject()
    service.setIndex(0)
    service.setIsDefault(true)
    service.names.add(serviceName())
    attributes.mapTo(service.requestedAttributes) { (name, required) ->
      requestedAttribute(name, required)
    }
    return service
  }

  /**
   * Shibboleth falls back to this as the display name when no `mdui:DisplayName` is published, so
   * it is what a user sees on the consent screen.
   */
  private fun serviceName(): ServiceName {
    val name = ServiceNameBuilder().buildObject()
    name.setValue(productName)
    name.setXMLLang(SERVICE_NAME_LANGUAGE)
    return name
  }
}

/** A relying party's descriptor is the only role Spring puts into the document. */
private fun serviceProviderDescriptor(parameters: EntityDescriptorParameters): SPSSODescriptor =
    parameters.entityDescriptor.roleDescriptors.filterIsInstance<SPSSODescriptor>().single()

/**
 * Follows the registration's own flag rather than stating outright what we would like to be true. A
 * document declaring signed requests while unsigned ones are sent is rejected by a conforming
 * identity provider, so the declaration and the behaviour must not be able to disagree.
 *
 * An absent attribute already means `false` per SAML 2.0 Metadata, so a registration which opted
 * out of signing declares nothing.
 */
private fun declareAuthnRequestsSigned(
    registration: RelyingPartyRegistration,
    descriptor: SPSSODescriptor
) {
  if (registration.isAuthnRequestsSigned) {
    descriptor.setAuthnRequestsSigned(true)
  }
}

/**
 * The attributes to ask for, mapped to whether the login fails without them, in the order they are
 * published.
 *
 * Keyed by attribute name across the whole set rather than only checking the extras against the
 * derived ones: two mappings may legitimately point at the same attribute, which would otherwise be
 * requested twice with conflicting `isRequired` values. The ID is inserted first so a required
 * entry survives the collision.
 */
private fun requestedAttributes(registration: ExtendedRegistration): Map<String, Boolean> {
  val mapping = registration.attributeMapping
  val attributes = LinkedHashMap<String, Boolean>()
  if (mapping.id !in SUBJECT_ID_REQUIREMENTS) {
    attributes[mapping.id] = true
  }
  for (name in listOf(mapping.mailAddress, mapping.givenName, mapping.surname)) {
    attributes.putIfAbsent(name, false)
  }
  for (attribute in registration.additionalRequestedAttributes) {
    attributes.putIfAbsent(attribute.name, attribute.required)
  }
  return attributes
}

/**
 * No `FriendlyName`: identity providers match on `Name`, and a value disagreeing with the
 * federation registry is a common validator complaint.
 *
 * An absent `NameFormat` already means `unspecified` per SAML 2.0 Core, so only a URI-shaped name
 * needs one stated.
 */
private fun requestedAttribute(name: String, required: Boolean): RequestedAttribute {
  val attribute = RequestedAttributeBuilder().buildObject()
  attribute.setName(name)
  if (name.startsWith("urn:") || name.contains("://")) {
    attribute.setNameFormat(Attribute.URI_REFERENCE)
  }
  attribute.setIsRequired(required)
  return attribute
}

private fun declareSubjectIdRequirement(entityDescriptor: EntityDescriptor, requirement: String) {
  val attribute = AttributeBuilder().buildObject()
  attribute.setName(SUBJECT_ID_REQUIREMENT_ATTRIBUTE)
  attribute.setNameFormat(Attribute.URI_REFERENCE)
  attribute.attributeValues.add(attributeValue(requirement))
  val entityAttributes = EntityAttributesBuilder().buildObject()
  entityAttributes.attributes.add(attribute)
  val extensions =
      entityDescriptor.extensions
          ?: ExtensionsBuilder().buildObject().also { entityDescriptor.extensions = it }
  extensions.unknownXMLObjects.add(entityAttributes)
}

/**
 * `XSAny` rather than `XSString`: the marshaller is looked up by element name, and the one
 * registered for `AttributeValue` casts what it is given to [XSAny]. Built without a schema type,
 * so no `xsi:type` is emitted for a plain string value.
 */
private fun attributeValue(value: String): XSAny {
  val element = AttributeValue.DEFAULT_ELEMENT_NAME
  val attributeValue =
      XSAnyBuilder().buildObject(element.namespaceURI, element.localPart, element.prefix)
  attributeValue.setTextContent(value)
  return attributeValue
}
