/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
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
    val authentication =
        this.delegate.convert(responseToken) ?: error("Conversion of SAML response failed.")
    val saml2Principal = authentication.principal as Saml2AuthenticatedPrincipal
    val providerId = UUID.fromString(saml2Principal.relyingPartyRegistrationId)
    val registration = saml2Properties.registration[providerId]!!
    val attributes = saml2Principal.attributes
    logger.debug("Received SAML attributes: {}", attributes.keys)
    val idMappingAttribute = registration.attributeMapping.id
    val externalId =
        attributes[idMappingAttribute]?.firstOrNull()?.toString()
            ?: error("Required SAML attribute $idMappingAttribute is missing.")
    logger.debug("Using SAML attribute {} as ID: {}", idMappingAttribute, externalId)
    var principal = this.userService.loadUserByProviderIdAndExternalId(providerId, externalId)
    if (principal == null) {
      logger.info("Creating new account for SAML user {}...", externalId)
      principal = updateUserFromAttributes(providerId, User(), attributes)
      val externalLogin = ExternalLogin(providerId = providerId, externalId = externalId)
      userService.createForExternalLogin(principal, externalLogin)
    } else {
      val externalLogin = principal.externalLogins.find { it.providerId == providerId }!!
      externalLogin.lastLoginAt = Instant.now()
      externalLoginRepository.save(externalLogin)
      updateUserFromAttributes(providerId, principal, attributes)
      userRepository.save(principal)
    }
    return Saml2Authentication(principal, authentication.saml2Response, principal.authorities)
  }

  private fun updateUserFromAttributes(
      providerId: UUID,
      user: User,
      attributes: Map<String, List<Any>>
  ): User {
    val mapping = saml2Properties.registration[providerId]!!.attributeMapping
    user.mailAddress = attributes[mapping.mailAddress]?.firstOrNull()?.toString()
    logger.debug(
        "Mapped SAML attribute {} to mailAddress: {}", mapping.mailAddress, user.mailAddress)
    user.givenName = attributes[mapping.givenName]?.firstOrNull()?.toString()
    logger.debug("Mapped SAML attribute {} to givenName: {}", mapping.givenName, user.givenName)
    user.surname = attributes[mapping.surname]?.firstOrNull()?.toString()
    logger.debug("Mapped SAML attribute {} to surname: {}", mapping.surname, user.surname)
    return user
  }
}
