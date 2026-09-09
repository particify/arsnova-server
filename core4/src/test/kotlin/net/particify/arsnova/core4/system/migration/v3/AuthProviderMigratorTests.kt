/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.net.URI
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

private const val LOCAL_LOGIN_ID = "migrated.local@example.com"
private const val MIXED_CASE_LOGIN_ID = "Migrated.External"
private const val MAIL_ADDRESS = "migrated.person@example.com"
private const val GIVEN_NAME = "Ines"
private const val SURNAME = "Adeyemi"

private val PROVIDER_IDS =
    mapOf(
        UserProfile.AuthProvider.CAS to UUID.fromString("a1d3f5e7-1111-4a2b-8c3d-4e5f60718293"),
        UserProfile.AuthProvider.LDAP to UUID.fromString("b2e4f6a8-2222-4b3c-9d4e-5f6071829304"),
        UserProfile.AuthProvider.OIDC to UUID.fromString("c3f507b9-3333-4c4d-8e5f-60718293a4b5"),
        UserProfile.AuthProvider.SAML to UUID.fromString("d40618ca-4444-4d5e-9f60-718293a4b5c6"))

/**
 * The identity rules per v3 authentication provider. They decide what a migrated account is
 * permanently called, so every branch is pinned here rather than only through a migration run.
 */
class AuthProviderMigratorTests {
  @Test
  fun shouldWriteMailAddressFromLoginIdForLocalProvider() {
    val user = newUser(LOCAL_LOGIN_ID)
    val migrated =
        migrator()
            .migrateIdentity(user, userProfile(UserProfile.AuthProvider.ARSNOVA, LOCAL_LOGIN_ID))
    Assertions.assertTrue(migrated)
    Assertions.assertEquals(LOCAL_LOGIN_ID, user.mailAddress)
    Assertions.assertEquals(LOCAL_LOGIN_ID, user.username)
    Assertions.assertNull(user.unverifiedMailAddress)
    Assertions.assertTrue(user.externalLogins.isEmpty())
  }

  @ParameterizedTest
  @EnumSource(UserProfile.AuthProvider::class, names = ["CAS", "LDAP", "OIDC", "SAML"])
  fun shouldParkMailAddressForExternalProvider(authProvider: UserProfile.AuthProvider) {
    val user = newUser(MIXED_CASE_LOGIN_ID)
    val migrated = migrator().migrateIdentity(user, userProfile(authProvider, MIXED_CASE_LOGIN_ID))
    Assertions.assertTrue(migrated)
    Assertions.assertEquals(MAIL_ADDRESS, user.unverifiedMailAddress)
    Assertions.assertNull(user.mailAddress)
    Assertions.assertNull(user.username)
    Assertions.assertEquals(PROVIDER_IDS[authProvider], user.externalLogins.single().providerId)
  }

  /** Expected values are spelled out rather than derived, so the rule itself is asserted. */
  @ParameterizedTest
  @MethodSource("externalIdCases")
  fun shouldLowercaseExternalIdForLdapOnly(
      authProvider: UserProfile.AuthProvider,
      expectedExternalId: String
  ) {
    val user = newUser(MIXED_CASE_LOGIN_ID)
    migrator().migrateIdentity(user, userProfile(authProvider, MIXED_CASE_LOGIN_ID))
    Assertions.assertEquals(expectedExternalId, user.externalLogins.single().externalId)
  }

  /** A guest account must not carry an address at all: even a parked one becomes its display ID. */
  @Test
  fun shouldLeaveGuestWithoutAnyMailAddress() {
    val user = newUser(LOCAL_LOGIN_ID)
    val migrated =
        migrator()
            .migrateIdentity(
                user, userProfile(UserProfile.AuthProvider.ARSNOVA_GUEST, LOCAL_LOGIN_ID))
    Assertions.assertTrue(migrated)
    Assertions.assertNull(user.mailAddress)
    Assertions.assertNull(user.unverifiedMailAddress)
    Assertions.assertNull(user.username)
    Assertions.assertTrue(user.externalLogins.isEmpty())
  }

  @Test
  fun shouldClearAnonymizedAccountForSoftDelete() {
    val user = newUser(LOCAL_LOGIN_ID)
    val migrated =
        migrator()
            .migrateIdentity(user, userProfile(UserProfile.AuthProvider.ANONYMIZED, LOCAL_LOGIN_ID))
    Assertions.assertTrue(migrated)
    Assertions.assertNull(user.username)
    Assertions.assertNull(user.mailAddress)
    Assertions.assertNull(user.unverifiedMailAddress)
    Assertions.assertNull(user.givenName)
    Assertions.assertNull(user.surname)
    Assertions.assertNotNull(user.deletedAt)
  }

  @ParameterizedTest
  @EnumSource(UserProfile.AuthProvider::class, names = ["NONE", "UNKNOWN"])
  fun shouldSkipUnsupportedProvider(authProvider: UserProfile.AuthProvider) {
    val user = newUser(LOCAL_LOGIN_ID)
    val migrated = migrator().migrateIdentity(user, userProfile(authProvider, LOCAL_LOGIN_ID))
    Assertions.assertFalse(migrated)
    Assertions.assertTrue(user.externalLogins.isEmpty())
  }

  /**
   * Without the registration the username mapping is unknown, and a guessed username would be
   * permanent, so the account is skipped instead.
   */
  @Test
  fun shouldSkipSamlProviderWithoutRegistration() {
    val user = newUser(MIXED_CASE_LOGIN_ID)
    val migrator = migrator(registrations = mapOf())
    val migrated =
        migrator.migrateIdentity(
            user, userProfile(UserProfile.AuthProvider.SAML, MIXED_CASE_LOGIN_ID))
    Assertions.assertFalse(migrated)
    Assertions.assertTrue(user.externalLogins.isEmpty())
  }

  @ParameterizedTest
  @EnumSource(UserProfile.AuthProvider::class, names = ["CAS", "LDAP", "OIDC", "SAML"])
  fun shouldSkipExternalProviderWithoutIdMapping(authProvider: UserProfile.AuthProvider) {
    val user = newUser(MIXED_CASE_LOGIN_ID)
    val migrator = migrator(providerMapping = mapOf())
    val migrated = migrator.migrateIdentity(user, userProfile(authProvider, MIXED_CASE_LOGIN_ID))
    Assertions.assertFalse(migrated)
    Assertions.assertTrue(user.externalLogins.isEmpty())
  }

  private fun migrator(
      providerMapping: Map<String, UUID> =
          PROVIDER_IDS.entries.associate { it.key.name to it.value },
      registrations: Map<UUID, ExtendedRegistration> =
          mapOf(PROVIDER_IDS.getValue(UserProfile.AuthProvider.SAML) to ExtendedRegistration())
  ) =
      AuthProviderMigrator(
          MigrationProperties(
              enabled = true,
              couchdb =
                  MigrationProperties.Couchdb(
                      URI.create("http://couchdb.invalid"), "migration", "migration"),
              roomAccessUrl = URI.create("http://authz.invalid"),
              authenticationProviderMapping = providerMapping),
          ExtendedSaml2RelyingPartyProperties(registrations))

  /** Mirrors how [Migrator] populates an account before the provider rules are applied. */
  private fun newUser(loginId: String) =
      User(
          username = loginId, mailAddress = MAIL_ADDRESS, givenName = GIVEN_NAME, surname = SURNAME)

  private fun userProfile(authProvider: UserProfile.AuthProvider, loginId: String) =
      UserProfile(
          id = UUID.randomUUID().toString(),
          creationTimestamp = Instant.now(),
          updateTimestamp = null,
          lastActivityTimestamp = Instant.now(),
          authProvider = authProvider,
          loginId = loginId,
          account = null,
          person = UserProfile.Person(MAIL_ADDRESS, GIVEN_NAME, SURNAME, null),
          settings = null,
          announcementReadTimestamp = null)

  private companion object {
    @JvmStatic
    fun externalIdCases() =
        listOf(
            Arguments.of(UserProfile.AuthProvider.CAS, MIXED_CASE_LOGIN_ID),
            Arguments.of(UserProfile.AuthProvider.LDAP, "migrated.external"),
            Arguments.of(UserProfile.AuthProvider.OIDC, MIXED_CASE_LOGIN_ID),
            Arguments.of(UserProfile.AuthProvider.SAML, MIXED_CASE_LOGIN_ID))
  }
}
