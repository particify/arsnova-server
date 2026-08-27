/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.time.Instant
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExternalLogin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Applies the identity rules of the v3 authentication provider an account was created under, and
 * reports whether the account can be migrated at all.
 *
 * Extracted from [Migrator] so the rules can be exercised without a database, an HTTP client or a
 * CouchDB instance: they decide what an account is permanently called, and a wrong decision cannot
 * be taken back once users have logged in.
 */
@Component
class AuthProviderMigrator(
    private val properties: MigrationProperties,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun migrateIdentity(newUser: User, userProfile: UserProfile): Boolean {
    return when (userProfile.authProvider) {
      UserProfile.AuthProvider.ANONYMIZED -> {
        newUser.clearForSoftDelete()
        newUser.deletedAt = Instant.now()
        true
      }
      UserProfile.AuthProvider.ARSNOVA_GUEST -> {
        // A guest account carries no mail address at all, not even an unverified one: that would
        // still surface as the account's display ID.
        newUser.mailAddress = null
        newUser.unverifiedMailAddress = null
        newUser.username = null
        true
      }
      UserProfile.AuthProvider.ARSNOVA -> {
        newUser.mailAddress = userProfile.loginId
        true
      }
      UserProfile.AuthProvider.CAS,
      UserProfile.AuthProvider.LDAP,
      UserProfile.AuthProvider.OIDC,
      UserProfile.AuthProvider.SAML -> migrateExternalLogin(newUser, userProfile)
      else -> {
        logger.warn("Unsupported authentication provider: {}", userProfile.authProvider)
        false
      }
    }
  }

  /**
   * Leaves the unique columns empty and parks the mail address instead of writing it: the account
   * which keeps an address shared by several users cannot be determined while documents are
   * streamed in, so [UserMigrationPostProcessor] settles both columns once all users exist.
   */
  private fun migrateExternalLogin(newUser: User, userProfile: UserProfile): Boolean {
    val authProvider = userProfile.authProvider
    val providerId = properties.authenticationProviderMapping[authProvider.name]
    if (providerId == null) {
      logger.warn("No ID mapping for authentication provider found: {}", authProvider)
      return false
    }
    if (authProvider == UserProfile.AuthProvider.SAML &&
        saml2Properties.registration[providerId] == null) {
      logger.warn(
          "No SAML registration found for provider {}. Without it the username mapping is " +
              "unknown, and a wrong username would be permanent.",
          providerId)
      return false
    }
    newUser.unverifiedMailAddress = newUser.mailAddress
    newUser.mailAddress = null
    newUser.username = null
    newUser.externalLogins +=
        ExternalLogin(
            user = newUser,
            providerId = providerId,
            externalId = externalId(authProvider, userProfile.loginId),
            auditMetadata = AuditMetadata(createdAt = Instant.now()))
    return true
  }

  /**
   * v3 lowercased login IDs for local accounts only, while core4 canonicalizes LDAP user IDs to
   * lowercase. A mixed-case value would not be found on the first login, and a second account would
   * be created for the same user.
   */
  private fun externalId(authProvider: UserProfile.AuthProvider, loginId: String): String =
      if (authProvider == UserProfile.AuthProvider.LDAP) loginId.lowercase() else loginId
}
