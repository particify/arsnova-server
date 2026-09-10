/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
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
 * and recipient have to match this exactly.
 */
fun acsLocation(registrationId: UUID) = "http://localhost/login/saml2/sso/$registrationId"

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
 * even though no request is ever sent to the identity provider.
 */
fun registerRelyingParty(
    registry: DynamicPropertyRegistry,
    identityProvider: Saml2TestIdentityProvider,
    registrationId: String,
    usernameMapping: String? = null
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

private fun relyingPartyPrefix(registrationId: String) =
    "security.saml2.relyingparty.registration.$registrationId"
