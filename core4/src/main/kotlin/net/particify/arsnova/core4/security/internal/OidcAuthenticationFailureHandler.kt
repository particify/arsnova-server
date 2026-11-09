/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

/** Where a login is started and where the provider sends the browser back to. */
internal const val OIDC_AUTHORIZATION_PATH = "/oauth2/authorization/{registrationId}"
internal const val OIDC_REDIRECT_PATH = "/login/oauth2/code/{registrationId}"

/** RFC 6749's charset for `error`, which rules out CR and LF, with a bound on the length. */
private val ERROR_CODE = Regex("[\\x20\\x21\\x23-\\x5B\\x5D-\\x7E]{1,64}")

private const val INVALID_ERROR_CODE = "invalid_error"

/**
 * Covers both ends of a login: starting one, which fails when the provider's discovery document
 * cannot be read, and the provider's response.
 */
class OidcAuthenticationFailureHandler(private val registrationIds: Set<UUID>) :
    ExternalLoginFailureHandler("OIDC") {
  private val paths =
      listOf(OIDC_AUTHORIZATION_PATH, OIDC_REDIRECT_PATH).map {
        PathPatternRequestMatcher.pathPattern(it)
      }

  /**
   * The provider's `error` parameter arrives here as it was sent, and anyone can start a login to
   * obtain a `state` it is accepted with, so a code outside the charset is replaced.
   */
  override fun errorCode(exception: AuthenticationException): String? =
      (exception as? OAuth2AuthenticationException)?.error?.errorCode?.let {
        if (ERROR_CODE.matches(it)) it else INVALID_ERROR_CODE
      }

  override fun registrationId(request: HttpServletRequest): UUID? =
      paths
          .firstNotNullOfOrNull { it.matcher(request).variables["registrationId"] }
          ?.let(::parseUuid)
          ?.takeIf { it in registrationIds }

  private fun parseUuid(value: String): UUID? =
      try {
        UUID.fromString(value)
      } catch (_: IllegalArgumentException) {
        null
      }
}
