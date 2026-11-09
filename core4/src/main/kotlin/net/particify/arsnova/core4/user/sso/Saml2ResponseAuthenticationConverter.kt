/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.sso

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.ExternalLoginRefusedException
import net.particify.arsnova.core4.user.internal.ExternalIdentity
import net.particify.arsnova.core4.user.internal.ExternalLoginProvisioningService
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class Saml2ResponseAuthenticationConverter(
    private val provisioningService: ExternalLoginProvisioningService,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) : Converter<ResponseToken, Saml2Authentication> {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val delegate: ResponseAuthenticationConverter = ResponseAuthenticationConverter()

  /**
   * The transaction spans the session authentication as well, which reads the account's roles. A
   * refusal is translated because the authentication provider reports any other exception under a
   * generic error code.
   */
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
    val identity = mapIdentity(providerId, registration, credentials.attributes)
    val principal =
        try {
          provisioningService.provision(
              identity, registration.usernameMapping, registration.linkingStrategy)
        } catch (e: ExternalLoginRefusedException) {
          throw Saml2AuthenticationException(Saml2Error(e.errorCode, e.message), e)
        }
    return Saml2SessionAuthentication(
        principal,
        authentication.saml2Response,
        principal.authorities,
        sessionEndsAt(responseToken))
  }

  private fun mapIdentity(
      providerId: UUID,
      registration: ExtendedRegistration,
      attributes: Map<String, List<Any>>
  ): ExternalIdentity {
    logger.debug("Received SAML attributes: {}", attributes.keys)
    val mapping = registration.attributeMapping
    val externalId =
        firstValue(attributes, mapping.id)
            ?: missingIdAttribute(providerId, mapping.id, attributes.keys)
    logger.debug("Using SAML attribute {} as ID: {}", mapping.id, externalId)
    val identity =
        ExternalIdentity(
            registrationId = providerId,
            externalId = externalId,
            mailAddress = firstValue(attributes, mapping.mailAddress),
            givenName = firstValue(attributes, mapping.givenName),
            surname = firstValue(attributes, mapping.surname),
            attributes = attributes)
    logger.debug(
        "Mapped SAML attributes {}, {} and {} to mailAddress {}, givenName {} and surname {}.",
        mapping.mailAddress,
        mapping.givenName,
        mapping.surname,
        identity.mailAddress,
        identity.givenName,
        identity.surname)
    return identity
  }

  private fun firstValue(attributes: Map<String, List<Any>>, name: String): String? =
      attributes[name]?.firstOrNull()?.toString()

  /**
   * Spring logs the authentication failure this becomes at DEBUG, which suits a wrong password and
   * not a configuration fault failing every login. Attribute names only: the values are personal
   * data and this line is on by default.
   */
  private fun missingIdAttribute(
      providerId: UUID,
      idMappingAttribute: String,
      receivedAttributes: Set<String>
  ): Nothing {
    logger.warn(
        "SAML registration {}: no login can succeed because the assertion carries no attribute " +
            "{}, which attribute-mapping.id names. Attributes received: {}.",
        providerId,
        idMappingAttribute,
        receivedAttributes)
    error("Required SAML attribute $idMappingAttribute is missing.")
  }

  /**
   * The earliest bound any assertion puts on the session itself. `Conditions.notOnOrAfter` is
   * deliberately not consulted as a fallback: it bounds how long the assertion may be replayed
   * rather than how long the session lasts, and is only minutes wide.
   */
  private fun sessionEndsAt(responseToken: ResponseToken): Instant? =
      responseToken.response.assertions
          .flatMap { it.authnStatements }
          .mapNotNull { it.sessionNotOnOrAfter }
          .minOrNull()
}
