/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.LdapProperties.ImportedAttribute
import net.particify.arsnova.core4.user.internal.LdapProperties.Registration
import org.hibernate.Hibernate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ldap.core.DirContextOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates and updates accounts for users authenticated against a directory. This is a bean of its
 * own because the [org.springframework.security.ldap.userdetails.UserDetailsContextMapper]s
 * delegating to it are constructed by hand and therefore cannot be transactional themselves.
 */
@Service
class LdapUserProvisioningService(
    private val userService: UserServiceImpl,
    private val userRepository: UserRepository,
    private val externalLoginRepository: ExternalLoginRepository,
    private val ldapProperties: LdapProperties
) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun provisionUser(providerId: UUID, ctx: DirContextOperations, submittedUsername: String): User {
    val registration =
        checkNotNull(ldapProperties.registration[providerId]) {
          "No LDAP registration for provider $providerId."
        }
    val externalId = canonicalId(registration, ctx, submittedUsername)
    val existingUser = userService.loadUserByProviderIdAndExternalId(providerId, externalId)
    val user =
        if (existingUser == null) createUser(providerId, registration, externalId, ctx)
        else updateUser(providerId, registration, existingUser, externalId, ctx)
    // The provider reads the authorities from the returned entity once this transaction has ended.
    Hibernate.initialize(user.roles)
    return user
  }

  /**
   * Determines the canonical ID from the directory instead of relying on the submitted username
   * which may differ in case.
   */
  private fun canonicalId(
      registration: Registration,
      ctx: DirContextOperations,
      submittedUsername: String
  ): String {
    val id = ctx.getStringAttribute(registration.userIdAttribute)
    if (id == null) {
      logger.warn(
          "LDAP attribute {} is not set. Falling back to the submitted username.",
          registration.userIdAttribute)
    }
    return (id ?: submittedUsername).lowercase()
  }

  private fun createUser(
      providerId: UUID,
      registration: Registration,
      externalId: String,
      ctx: DirContextOperations
  ): User {
    logger.info("Creating new account for LDAP user {}...", externalId)
    val user = User()
    assignUsername(user, externalId)
    updateUserFromAttributes(registration, user, ctx)
    val externalLogin = ExternalLogin(providerId = providerId, externalId = externalId)
    return userService.createForExternalLogin(user, externalLogin)
  }

  @Suppress("LongParameterList")
  private fun updateUser(
      providerId: UUID,
      registration: Registration,
      user: User,
      externalId: String,
      ctx: DirContextOperations
  ): User {
    val externalLogin = user.externalLogins.first { it.providerId == providerId }
    externalLogin.lastLoginAt = Instant.now()
    externalLoginRepository.save(externalLogin)
    // Accounts imported from v3 have no username, so it is backfilled on the next login.
    if (user.username == null) {
      assignUsername(user, externalId)
    }
    updateUserFromAttributes(registration, user, ctx)
    return userRepository.save(user)
  }

  /** Setting the username marks the account as verified. */
  private fun assignUsername(user: User, externalId: String) {
    if (userRepository.existsByUsername(externalId)) {
      logger.warn(
          "Username {} is already in use. Leaving the account of the LDAP user unverified.",
          externalId)
      return
    }
    user.username = externalId
  }

  private fun updateUserFromAttributes(
      registration: Registration,
      user: User,
      ctx: DirContextOperations
  ) {
    for (attributeName in registration.importedAttributes) {
      val value = ctx.getStringAttribute(attributeName)
      logger.debug("Mapping LDAP attribute {}: {}", attributeName, value)
      when (ImportedAttribute.byAttributeName(attributeName)) {
        ImportedAttribute.GIVEN_NAME -> user.givenName = value
        ImportedAttribute.MAIL -> updateMailAddress(user, value)
        ImportedAttribute.SURNAME -> user.surname = value
        // Unreachable: unsupported attributes are rejected when the properties are bound.
        null -> logger.warn("Skipping unsupported LDAP attribute {}.", attributeName)
      }
    }
  }

  private fun updateMailAddress(user: User, mailAddress: String?) {
    val normalized = mailAddress?.lowercase()
    if (normalized == null || normalized == user.mailAddress) {
      user.mailAddress = normalized
      return
    }
    if (userRepository.existsByMailAddress(normalized)) {
      logger.warn(
          "Mail address {} is already in use. Not importing it for the LDAP user.", normalized)
      return
    }
    user.mailAddress = normalized
  }
}
