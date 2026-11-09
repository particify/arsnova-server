/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties
import org.springframework.security.core.AuthenticationException
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

private const val REGISTRATION_ID_VARIABLE = "registrationId"

/**
 * The whole exception is logged by `Saml2WebSsoAuthenticationFilter` at TRACE; only the
 * `Saml2Error` code is shown.
 */
class Saml2AuthenticationFailureHandler(saml2Properties: ExtendedSaml2RelyingPartyProperties) :
    ExternalLoginFailureHandler("SAML") {
  private val registrationIds = saml2Properties.registration.keys
  private val configuredPaths =
      Saml2AssertionConsumerServices(saml2Properties).configuredPaths().map { (id, path) ->
        PathPatternRequestMatcher.pathPattern(path) to id
      }
  private val defaultPath =
      PathPatternRequestMatcher.pathPattern(
          DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION.removePrefix("{baseUrl}"))

  override fun errorCode(exception: AuthenticationException): String? =
      (exception as? Saml2AuthenticationException)?.saml2Error?.errorCode

  override fun registrationId(request: HttpServletRequest): UUID? =
      configuredPaths.firstOrNull { (matcher) -> matcher.matches(request) }?.second
          ?: defaultPath
              .matcher(request)
              .variables[REGISTRATION_ID_VARIABLE]
              ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
              ?.takeIf { it in registrationIds }
}
