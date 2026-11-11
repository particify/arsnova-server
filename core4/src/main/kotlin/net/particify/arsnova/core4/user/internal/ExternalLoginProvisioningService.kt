/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import net.particify.arsnova.core4.user.ExternalLoginHook
import net.particify.arsnova.core4.user.MailAddressValidation
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.sso.UsernameMapping
import net.particify.arsnova.core4.user.sso.resolveUsername
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates or updates the account behind a single sign-on login, independent of the protocol which
 * carried it, and calls the [ExternalLoginHook]s around that.
 *
 * External IDs, mail addresses and usernames are personal data, so they are logged at DEBUG only:
 * the INFO and WARN lines say what happened, and the DEBUG line following each names the values.
 */
@Service
class ExternalLoginProvisioningService(
    private val userService: UserServiceImpl,
    private val userRepository: UserRepository,
    private val externalLoginRepository: ExternalLoginRepository,
    private val linkingStrategyRegistry: ExternalLoginLinkingStrategyRegistry,
    private val mailAddressValidation: MailAddressValidation,
    private val loginHooks: List<ExternalLoginHook>
) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /** @param linkingStrategy the name of the strategy the registration selects, `null` for none. */
  @Transactional
  fun provision(
      identity: ExternalIdentity,
      usernameMapping: UsernameMapping,
      linkingStrategy: String?
  ): User {
    loginHooks.forEach { it.beforeProvisioning(identity.registrationId, identity.attributes) }
    val existingUser =
        userService.loadUserByProviderIdAndExternalId(identity.registrationId, identity.externalId)
    val user =
        if (existingUser == null) createUser(identity, usernameMapping, linkingStrategy)
        else updateUser(identity, usernameMapping, existingUser)
    loginHooks.forEach { it.afterProvisioning(user, identity.registrationId, identity.attributes) }
    return user
  }

  private fun createUser(
      identity: ExternalIdentity,
      usernameMapping: UsernameMapping,
      linkingStrategy: String?
  ): User {
    val user = findLinkTarget(identity, linkingStrategy) ?: User()
    updateUserFromIdentity(user, identity)
    // A linked account keeps the username it is already known by, which is its public display ID.
    if (user.username == null) {
      assignUsername(user, usernameMapping, identity.externalId)
    }
    val externalLogin =
        ExternalLogin(providerId = identity.registrationId, externalId = identity.externalId)
    return userService.createForExternalLogin(user, externalLogin)
  }

  /**
   * The asserted address is passed on as it is. [updateMailAddress] drops one which another account
   * already holds, so a strategy consulted after the account is populated never sees it.
   */
  private fun findLinkTarget(identity: ExternalIdentity, linkingStrategy: String?): User? {
    val linkTarget =
        linkingStrategyRegistry
            .find(linkingStrategy)
            ?.findLinkTarget(identity.registrationId, identity.externalId, identity.mailAddress)
    if (linkTarget == null) {
      logger.info("Creating new account for a login via registration {}.", identity.registrationId)
      logger.debug("Creating new account for external user {}.", identity.externalId)
    } else {
      logger.info(
          "Attaching a login via registration {} to existing account {}.",
          identity.registrationId,
          linkTarget.id)
      logger.debug(
          "Attaching external user {} to existing account {}.", identity.externalId, linkTarget.id)
    }
    return linkTarget
  }

  private fun updateUser(
      identity: ExternalIdentity,
      usernameMapping: UsernameMapping,
      user: User
  ): User {
    val externalLogin = user.externalLogins.first { it.providerId == identity.registrationId }
    externalLogin.lastLoginAt = Instant.now()
    externalLoginRepository.save(externalLogin)
    updateUserFromIdentity(user, identity)
    // Accounts imported from v3 and accounts created before a mapping was configured have no
    // username, so it is backfilled on the next login.
    if (user.username == null) {
      assignUsername(user, usernameMapping, identity.externalId)
    }
    return userRepository.save(user)
  }

  /** Setting the username marks the account as verified. */
  internal fun assignUsername(user: User, usernameMapping: UsernameMapping, externalId: String) {
    val username = resolveUsername(usernameMapping, externalId, user.mailAddress)
    if (username == null) {
      logger.warn(
          "No value available for username mapping {}. Leaving the account unverified.",
          usernameMapping)
      logger.debug("No username for external user {}.", externalId)
      return
    }
    // An address imported before addresses were validated must not become the username either.
    if (usernameMapping == UsernameMapping.MAIL_ADDRESS &&
        !mailAddressValidation.isValid(username)) {
      logger.warn("Mail address is not valid. Leaving the account unverified.")
      logger.debug(
          "Mail address {} of external user {} is not valid as a username.", username, externalId)
      return
    }
    if (userRepository.existsByUsername(username)) {
      logger.warn("Username is already in use. Leaving the account unverified.")
      logger.debug("Username {} of external user {} is already in use.", username, externalId)
      return
    }
    user.username = username
  }

  private fun updateUserFromIdentity(user: User, identity: ExternalIdentity) {
    updateMailAddress(user, identity.mailAddress)
    if (!identity.givenName.isNullOrBlank()) {
      user.givenName = identity.givenName
    }
    if (!identity.surname.isNullOrBlank()) {
      user.surname = identity.surname
    }
  }

  /**
   * An asserted address which is not valid or which another account already holds is skipped
   * instead of failing the login: the unique constraint would otherwise reject the account on every
   * attempt, and an invalid one would be mailed and shown as the account's address. An assertion
   * carrying no address at all leaves the stored one untouched, so an identity provider which stops
   * releasing the attribute does not clear the address of every account which logs in afterwards.
   */
  internal fun updateMailAddress(user: User, mailAddress: String?) {
    val normalized = mailAddress?.lowercase() ?: return
    if (normalized == user.mailAddress) {
      return
    }
    if (!mailAddressValidation.isValid(normalized)) {
      logger.warn("Mail address is not valid. Not importing it for the external user.")
      logger.debug("Mail address {} is not valid.", normalized)
      return
    }
    if (userRepository.existsByMailAddress(normalized)) {
      logger.warn("Mail address is already in use. Not importing it for the external user.")
      logger.debug("Mail address {} is already in use.", normalized)
      return
    }
    user.mailAddress = normalized
  }
}
