/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.security.cert.X509Certificate
import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.test.context.DynamicPropertyRegistry

const val SAML_IDP_ENTITY_ID = "https://idp.example.com/saml2"

/** The attribute names the converter falls back to when no `attribute-mapping` is configured. */
const val SAML_ID_ATTRIBUTE = "urn:oasis:names:tc:SAML:attribute:subject-id"
const val SAML_MAIL_ATTRIBUTE = "urn:oid:0.9.2342.19200300.100.1.3"
const val SAML_GIVEN_NAME_ATTRIBUTE = "urn:oid:2.5.4.42"
const val SAML_SURNAME_ATTRIBUTE = "urn:oid:2.5.4.4"

fun spEntityId(registrationId: UUID) = "https://sp.example.com/saml2/metadata/$registrationId"

/**
 * Where the assertion consumer sits for MockMvc, which serves on `localhost` without a context
 * path. Spring resolves the relying party's location from the request, so the asserted destination
 * and recipient have to match this exactly. [path] follows a registration which configures
 * `acs.location`.
 */
fun acsLocation(registrationId: UUID, path: String = "/login/saml2/sso/$registrationId") =
    "http://localhost$path"

/** A user the in-test identity provider can assert. A null attribute is left out entirely. */
data class Saml2TestUser(
    val subjectId: String,
    val mailAddress: String? = null,
    val givenName: String? = null,
    val surname: String? = null
) {
  /** Only the attributes which are actually present, keyed by the converter's default names. */
  val attributes: Map<String, String>
    get() = buildMap {
      put(SAML_ID_ATTRIBUTE, subjectId)
      mailAddress?.let { put(SAML_MAIL_ATTRIBUTE, it) }
      givenName?.let { put(SAML_GIVEN_NAME_ATTRIBUTE, it) }
      surname?.let { put(SAML_SURNAME_ATTRIBUTE, it) }
    }

  /** The same user as asserted by an identity provider which releases no mail attribute. */
  fun withoutMailAddress() = copy(mailAddress = null)
}

/**
 * Points one relying party registration at [identityProvider]. The signing credential is mandatory
 * -- the repository refuses a registration without one -- even though no request is ever sent to
 * the identity provider here.
 */
fun registerRelyingParty(
    registry: DynamicPropertyRegistry,
    identityProvider: Saml2TestIdentityProvider,
    registrationId: String,
    usernameMapping: String? = null,
    acsLocation: String? = null,
    metadataPath: String? = null
) {
  val prefix = relyingPartyPrefix(registrationId)
  registry.add("$prefix.entity-id") { spEntityId(UUID.fromString(registrationId)) }
  registry.add("$prefix.signing.credentials[0].private-key-location") {
    identityProvider.privateKeyLocation
  }
  registry.add("$prefix.signing.credentials[0].certificate-location") {
    identityProvider.certificateLocation
  }
  registry.add("$prefix.assertingparty.metadata-uri") { identityProvider.metadataLocation }
  usernameMapping?.let { registry.add("$prefix.username-mapping") { it } }
  acsLocation?.let { registry.add("$prefix.acs.location") { it } }
  metadataPath?.let { registry.add("$prefix.metadata-path") { it } }
}

/** How a registration is presented on the login page, which no login itself depends on. */
fun registerRelyingPartyDisplay(
    registry: DynamicPropertyRegistry,
    registrationId: String,
    title: String,
    order: Int
) {
  val prefix = relyingPartyPrefix(registrationId)
  registry.add("$prefix.title") { title }
  registry.add("$prefix.order") { order }
}

/**
 * The same registration as [registerRelyingParty] describes, bound already, for a test which drives
 * the registration repository without a Spring context.
 */
fun relyingPartyProperties(
    registrationId: UUID,
    identityProvider: Saml2TestIdentityProvider,
    metadataUri: String = identityProvider.metadataLocation,
    assertingPartyEntityId: String? = null,
    metadataVerificationCertificates: List<X509Certificate> = emptyList()
): ExtendedSaml2RelyingPartyProperties {
  val resourceLoader = DefaultResourceLoader()
  val credential =
      Saml2RelyingPartyProperties.Registration.Signing.Credential().apply {
        privateKeyLocation = resourceLoader.getResource(identityProvider.privateKeyLocation)
        certificateLocation = resourceLoader.getResource(identityProvider.certificateLocation)
      }
  val registration =
      ExtendedRegistration().apply {
        entityId = spEntityId(registrationId)
        assertingparty.entityId = assertingPartyEntityId
        assertingparty.metadataUri = metadataUri
        signing.credentials.add(credential)
        metadataVerification.credentials.addAll(
            metadataVerificationCertificates.map(::verificationCredential))
      }
  return ExtendedSaml2RelyingPartyProperties(mapOf(registrationId to registration))
}

private fun relyingPartyPrefix(registrationId: String) =
    "security.saml2.relyingparty.registration.$registrationId"

/** Pins a certificate without writing it out: the binder accepts any Spring resource. */
private fun verificationCredential(certificate: X509Certificate) =
    ExtendedRegistration.MetadataVerification.Credential().apply {
      certificateLocation = ByteArrayResource(certificate.encoded)
    }
