/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar

/** Registration whose username mapping is left at its `MAIL_ADDRESS` default. */
const val SAML_MAIL_REGISTRATION_ID = "b1f0a4d6-2c3e-4a58-9f21-0c7d5e8a1b34"

/** Registration configured with `username-mapping: ID`. */
const val SAML_ID_REGISTRATION_ID = "c48e9b12-7d05-4e3a-8b6f-1a2c3d4e5f60"

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

/**
 * Its name attributes are dropped from the second assertion, so the stored ones have to survive.
 */
val SAML_RETAINED_NAME_USER =
    Saml2TestUser(
        subjectId = "saml-retained-name-subject",
        mailAddress = "saml.retained.name@example.com",
        givenName = "Solveig",
        surname = "Ibarra")

/** Its asserted address is given to another account before the login, so it cannot be imported. */
val SAML_LINK_COLLISION_USER =
    Saml2TestUser(
        subjectId = "saml-link-collision-subject", mailAddress = "saml.link.collision@example.com")

/** Linked into an account which has no username yet. */
val SAML_LINK_TARGET_USER =
    Saml2TestUser(
        subjectId = "saml-link-target-subject", mailAddress = "saml.link.target@example.com")

/** Linked, through the `ID` registration, into an account whose username has to survive. */
val SAML_LINK_ID_USER =
    Saml2TestUser(subjectId = "Saml-Link-Id-Subject", mailAddress = "saml.link.id@example.com")

/** No link target is armed for it, so it is provisioned exactly as without any strategy. */
val SAML_UNLINKED_USER =
    Saml2TestUser(
        subjectId = "saml-unlinked-subject",
        mailAddress = "saml.unlinked@example.com",
        givenName = "Ferdinand",
        surname = "Okonkwo")

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
        registerRelyingParty(registry, identityProvider, SAML_MAIL_REGISTRATION_ID)
        registerRelyingParty(registry, identityProvider, SAML_ID_REGISTRATION_ID, "ID")
      }
}
