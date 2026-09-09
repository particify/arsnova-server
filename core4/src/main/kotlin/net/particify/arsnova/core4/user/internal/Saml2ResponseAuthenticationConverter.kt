/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class Saml2ResponseAuthenticationConverter(
    private val userService: UserServiceImpl,
    private val userRepository: UserRepository,
    private val externalLoginRepository: ExternalLoginRepository,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) : Converter<ResponseToken, Saml2Authentication> {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val delegate: ResponseAuthenticationConverter = ResponseAuthenticationConverter()

  @Transactional
  override fun convert(responseToken: ResponseToken): Saml2Authentication {
    logger.debug("Converting SAML response: {}", responseToken.response)
    val authentication = this.delegate.convert(responseToken)!!
    check(authentication is Saml2AssertionAuthentication) {
      "Unexpected type for Authentication object"
    }
    val credentials =
        checkNotNull(authentication.credentials) { "Authentication credentials must not be null." }
    val providerId = UUID.fromString(authentication.relyingPartyRegistrationId)
    val registration = saml2Properties.registration[providerId]!!
    val attributes = credentials.attributes
    logger.debug("Received SAML attributes: {}", attributes.keys)
    val idMappingAttribute = registration.attributeMapping.id
    val externalId =
        attributes[idMappingAttribute]?.firstOrNull()?.toString()
            ?: error("Required SAML attribute $idMappingAttribute is missing.")
    logger.debug("Using SAML attribute {} as ID: {}", idMappingAttribute, externalId)
    val existingUser = this.userService.loadUserByProviderIdAndExternalId(providerId, externalId)
    val principal =
        if (existingUser == null) createUser(providerId, registration, externalId, attributes)
        else updateUser(providerId, registration, existingUser, externalId, attributes)
    return Saml2Authentication(principal, authentication.saml2Response, principal.authorities)
  }

  private fun createUser(
      providerId: UUID,
      registration: ExtendedRegistration,
      externalId: String,
      attributes: Map<String, List<Any>>
  ): User {
    logger.info("Creating new account for SAML user {}...", externalId)
    val user = User()
    updateUserFromAttributes(registration, user, attributes)
    assignUsername(user, registration, externalId)
    val externalLogin = ExternalLogin(providerId = providerId, externalId = externalId)
    return userService.createForExternalLogin(user, externalLogin)
  }

  @Suppress("LongParameterList")
  private fun updateUser(
      providerId: UUID,
      registration: ExtendedRegistration,
      user: User,
      externalId: String,
      attributes: Map<String, List<Any>>
  ): User {
    val externalLogin = user.externalLogins.first { it.providerId == providerId }
    externalLogin.lastLoginAt = Instant.now()
    externalLoginRepository.save(externalLogin)
    updateUserFromAttributes(registration, user, attributes)
    // Accounts imported from v3 and accounts created before a mapping was configured have no
    // username, so it is backfilled on the next login.
    if (user.username == null) {
      assignUsername(user, registration, externalId)
    }
    return userRepository.save(user)
  }

  /** Setting the username marks the account as verified. */
  internal fun assignUsername(user: User, registration: ExtendedRegistration, externalId: String) {
    val username = resolveUsername(registration.usernameMapping, externalId, user.mailAddress)
    if (username == null) {
      logger.warn(
          "No value available for username mapping {}. Leaving the account of the SAML user {} " +
              "unverified.",
          registration.usernameMapping,
          externalId)
      return
    }
    if (userRepository.existsByUsername(username)) {
      logger.warn(
          "Username {} is already in use. Leaving the account of the SAML user unverified.",
          username)
      return
    }
    user.username = username
  }

  private fun updateUserFromAttributes(
      registration: ExtendedRegistration,
      user: User,
      attributes: Map<String, List<Any>>
  ): User {
    val mapping = registration.attributeMapping
    updateMailAddress(user, attributes[mapping.mailAddress]?.firstOrNull()?.toString())
    logger.debug(
        "Mapped SAML attribute {} to mailAddress: {}", mapping.mailAddress, user.mailAddress)
    user.givenName = attributes[mapping.givenName]?.firstOrNull()?.toString()
    logger.debug("Mapped SAML attribute {} to givenName: {}", mapping.givenName, user.givenName)
    user.surname = attributes[mapping.surname]?.firstOrNull()?.toString()
    logger.debug("Mapped SAML attribute {} to surname: {}", mapping.surname, user.surname)
    return user
  }

  /**
   * An asserted address which another account already holds is skipped instead of failing the
   * login: the unique constraint would otherwise reject the account on every attempt. An assertion
   * carrying no address at all leaves the stored one untouched, so an identity provider which stops
   * releasing the attribute does not clear the address of every account which logs in afterwards.
   */
  internal fun updateMailAddress(user: User, mailAddress: String?) {
    val normalized = mailAddress?.lowercase() ?: return
    if (normalized == user.mailAddress) {
      return
    }
    if (userRepository.existsByMailAddress(normalized)) {
      logger.warn(
          "Mail address {} is already in use. Not importing it for the SAML user.", normalized)
      return
    }
    user.mailAddress = normalized
  }
}
