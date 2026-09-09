/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.context.DynamicPropertyRegistry

/** Registration whose username mapping is left at its `MAIL_ADDRESS` default. */
const val SAML_MAIL_REGISTRATION_ID = "b1f0a4d6-2c3e-4a58-9f21-0c7d5e8a1b34"

/** Registration configured with `username-mapping: ID`. */
const val SAML_ID_REGISTRATION_ID = "c48e9b12-7d05-4e3a-8b6f-1a2c3d4e5f60"

const val SAML_IDP_ENTITY_ID = "https://idp.example.com/saml2"

/** The attribute names the converter falls back to when no `attribute-mapping` is configured. */
const val SAML_ID_ATTRIBUTE = "urn:oasis:names:tc:SAML:attribute:subject-id"
const val SAML_MAIL_ATTRIBUTE = "urn:oid:0.9.2342.19200300.100.1.3"
const val SAML_GIVEN_NAME_ATTRIBUTE = "urn:oid:2.5.4.42"
const val SAML_SURNAME_ATTRIBUTE = "urn:oid:2.5.4.4"

/** Account from the `dev` Liquibase fixtures, whose mail address a test asserts as a collision. */
const val SAML_TAKEN_MAIL_ADDRESS = "user@example.com"

val SAML_IMPORT_USER =
    Saml2TestUser(
        subjectId = "saml-import-subject",
        mailAddress = "saml.import.user@example.com",
        givenName = "Aurelie",
        surname = "Nakamura")

/** Mixed case on purpose: the mapping lowercases, which a real directory rarely lets us see. */
val SAML_MIXED_CASE_ID_USER =
    Saml2TestUser(subjectId = "Saml-Mixed-Case-Subject", mailAddress = "saml.mixed@example.com")

/** Asserts an address which the dev fixtures' account already holds. */
val SAML_MAIL_COLLISION_USER =
    Saml2TestUser(subjectId = "saml-mail-collision-subject", mailAddress = SAML_TAKEN_MAIL_ADDRESS)

val SAML_RETAINED_MAIL_USER =
    Saml2TestUser(
        subjectId = "saml-retained-mail-subject", mailAddress = "saml.retained@example.com")

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
 * Registers two relying parties against the in-test identity provider. Two are needed because the
 * username mapping is per registration, and driving both through one context is cheaper than a
 * second application context.
 */
@TestConfiguration(proxyBeanMethods = false)
class Saml2TestConfiguration {
  @Bean fun saml2TestIdentityProvider() = Saml2TestIdentityProvider()

  /** The generated credential paths are only known once the provider exists. */
  @Bean
  fun saml2PropertyRegistrar(identityProvider: Saml2TestIdentityProvider) =
      DynamicPropertyRegistrar { registry ->
        registerRelyingParty(registry, identityProvider, SAML_MAIL_REGISTRATION_ID, null)
        registerRelyingParty(registry, identityProvider, SAML_ID_REGISTRATION_ID, "ID")
      }

  private fun registerRelyingParty(
      registry: DynamicPropertyRegistry,
      identityProvider: Saml2TestIdentityProvider,
      registrationId: String,
      usernameMapping: String?
  ) {
    val prefix = "security.saml2.relyingparty.registration.$registrationId"
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
}
