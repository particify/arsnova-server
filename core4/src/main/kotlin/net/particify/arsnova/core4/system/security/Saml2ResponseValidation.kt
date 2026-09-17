/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.core.Saml2ErrorCodes
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.AssertionToken
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken

/**
 * Where a response says it was sent, and where the assertion says it may be presented, are both
 * checked against a single location Spring reads off the registration. A registration serving more
 * than one assertion consumer service would therefore advertise endpoints it then refuses
 * assertions at, which is what the two validators here widen.
 *
 * Both keep the stock rules and replace only the comparison, so nothing about status codes,
 * issuers, signatures, conditions or replay is reimplemented.
 */
class Saml2ResponseValidation(saml2Properties: ExtendedSaml2RelyingPartyProperties) {
  private val assertionConsumerServices = Saml2AssertionConsumerServices(saml2Properties)

  fun responseValidator(): Converter<ResponseToken, Saml2ResponseValidatorResult> =
      OpenSaml5AuthenticationProvider.ResponseValidator(
          OpenSaml5AuthenticationProvider.InResponseToValidator(),
          OpenSaml5AuthenticationProvider.IssuerValidator(),
          destinationValidator())

  /**
   * Assembled per assertion, because the consumer is handed the validation parameters alone: the
   * registration they were seeded from is not among them, and the recipients to accept depend on
   * it.
   */
  fun assertionValidator(): Converter<AssertionToken, Saml2ResponseValidatorResult> =
      Converter { token ->
        val recipients =
            assertionConsumerServices.acceptedLocations(token.token.relyingPartyRegistration)
        val validator =
            OpenSaml5AuthenticationProvider.createDefaultAssertionValidatorWithParameters {
                parameters ->
              parameters[SAML2AssertionValidationParameters.SC_VALID_RECIPIENTS] =
                  recipients.toSet()
            }
        validator.convert(token)
      }

  /**
   * Spring's own rule with the comparison widened, including its tolerance for a response carrying
   * no `Destination` at all -- which is legal for an unsigned response and which a stricter rule
   * here would start rejecting.
   */
  private fun destinationValidator(): Converter<ResponseToken, Saml2ResponseValidatorResult> =
      Converter { token ->
        val destination = token.response.destination
        val accepted =
            assertionConsumerServices.acceptedLocations(token.token.relyingPartyRegistration)
        if (destination.isNullOrEmpty() || destination in accepted) {
          Saml2ResponseValidatorResult.success()
        } else {
          Saml2ResponseValidatorResult.failure(
              Saml2Error(
                  Saml2ErrorCodes.INVALID_DESTINATION,
                  "Invalid destination [$destination] for SAML response [${token.response.id}]"))
        }
      }
}
