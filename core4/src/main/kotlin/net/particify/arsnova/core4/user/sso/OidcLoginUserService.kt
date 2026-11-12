/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.sso

import java.util.UUID
import net.particify.arsnova.core4.user.ExternalLoginRefusedException
import net.particify.arsnova.core4.user.internal.ExternalIdentity
import net.particify.arsnova.core4.user.internal.ExternalLoginProvisioningService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.oidc.StandardClaimNames
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

/** Shown when the ID claim is missing, which is a configuration fault on either side. */
private const val MISSING_ID_CLAIM_ERROR_CODE = "missing_id_claim"

/**
 * Provisions the account behind an OpenID Connect login from the claims of the ID token and, where
 * the provider offers it, the UserInfo endpoint.
 */
@Component
class OidcLoginUserService(
    private val provisioningService: ExternalLoginProvisioningService,
    private val oidcProperties: OidcProperties
) : OAuth2UserService<OidcUserRequest, OidcUser> {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val delegate = OidcUserService()

  override fun loadUser(userRequest: OidcUserRequest): OidcUser {
    val oidcUser = delegate.loadUser(userRequest)
    val registrationId = UUID.fromString(userRequest.clientRegistration.registrationId)
    val registration = checkNotNull(oidcProperties.registration[registrationId])
    val identity = mapIdentity(registrationId, registration, oidcUser.claims)
    return OidcLoginPrincipal(provision(identity, registration), oidcUser)
  }

  /**
   * Anything else which fails is reported as an authentication failure with the code
   * `server_error`, so the browser gets the same error page as for a refusal rather than the
   * container's, and the failure handler logs the code and the cause's class only: a persistence
   * error's message quotes the conflicting values.
   */
  @Suppress("TooGenericExceptionCaught")
  private fun provision(identity: ExternalIdentity, registration: OidcProperties.Registration) =
      try {
        provisioningService.provision(
            identity, registration.usernameMapping, registration.linkingStrategy)
      } catch (e: ExternalLoginRefusedException) {
        throw OAuth2AuthenticationException(OAuth2Error(e.errorCode, e.message, null), e)
      } catch (e: RuntimeException) {
        throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR), e)
      }

  private fun mapIdentity(
      registrationId: UUID,
      registration: OidcProperties.Registration,
      claims: Map<String, Any?>
  ): ExternalIdentity {
    logger.debug("Received OIDC claims: {}", claims.keys)
    val idClaim = registration.claimMapping.id
    val externalId =
        firstValue(claims, idClaim) ?: missingIdClaim(registrationId, idClaim, claims.keys)
    return ExternalIdentity(
        registrationId = registrationId,
        externalId = externalId,
        mailAddress = mailAddress(registrationId, registration, claims),
        givenName = firstValue(claims, StandardClaimNames.GIVEN_NAME),
        surname = firstValue(claims, StandardClaimNames.FAMILY_NAME),
        attributes = flatten(claims))
  }

  /**
   * An address the provider does not vouch for could be anyone's, so with the requirement in place
   * it is treated like one which was never sent.
   */
  private fun mailAddress(
      registrationId: UUID,
      registration: OidcProperties.Registration,
      claims: Map<String, Any?>
  ): String? {
    val mailAddress = firstValue(claims, StandardClaimNames.EMAIL) ?: return null
    if (registration.requireVerifiedMailAddress &&
        !isTrue(claims[StandardClaimNames.EMAIL_VERIFIED])) {
      logger.debug(
          "OIDC registration {}: ignoring the mail address, which the provider does not mark as " +
              "verified.",
          registrationId)
      return null
    }
    return mailAddress
  }

  /** Some providers send booleans as strings. */
  private fun isTrue(value: Any?): Boolean = value == true || value == "true"

  private fun firstValue(claims: Map<String, Any?>, name: String): String? =
      flattenValue(claims[name]).firstOrNull()?.toString()?.takeIf { it.isNotBlank() }

  /**
   * Claim names only: the values are personal data, and this line is on by default. The login fails
   * with an error code of its own, because the user can do nothing about it.
   */
  private fun missingIdClaim(
      registrationId: UUID,
      idClaim: String,
      receivedClaims: Set<String>
  ): Nothing {
    logger.warn(
        "OIDC registration {}: no login can succeed because the provider sends no claim {}, " +
            "which claim-mapping.id names. Claims received: {}.",
        registrationId,
        idClaim,
        receivedClaims)
    throw OAuth2AuthenticationException(
        OAuth2Error(MISSING_ID_CLAIM_ERROR_CODE, "Required claim $idClaim is missing.", null))
  }

  /**
   * Claims arrive as JSON values, while login hooks receive every attribute as a list of values, as
   * SAML asserts them: a scalar becomes a list of one, an array the list of its elements, and an
   * object a list of one map. A `null` value, or one inside an array, is dropped, and a claim left
   * without any value is omitted.
   */
  private fun flatten(claims: Map<String, Any?>): Map<String, List<Any>> =
      claims.mapValues { flattenValue(it.value) }.filterValues { it.isNotEmpty() }

  private fun flattenValue(value: Any?): List<Any> =
      when (value) {
        null -> listOf()
        is Collection<*> -> value.filterNotNull()
        is Array<*> -> value.filterNotNull()
        else -> listOf(value)
      }
}
