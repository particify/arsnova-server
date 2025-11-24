/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class UserJwtAuthenticationFilter(
    private val userJwtAuthenticationProvider: UserJwtAuthenticationProvider
) : OncePerRequestFilter() {
  companion object {
    private val BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE)
  }

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      chain: FilterChain
  ) {
    val jwtHeader =
        request.getHeader(HttpHeaders.AUTHORIZATION) ?: return chain.doFilter(request, response)

    val tokenMatcher: Matcher = BEARER_TOKEN_PATTERN.matcher(jwtHeader)
    if (!tokenMatcher.matches()) {
      logger.debug("Skipping JWT handling due to pattern mismatch.")
      return chain.doFilter(request, response)
    }
    val authentication = UserJwtAuthentication(tokenMatcher.group(1))

    try {
      val authenticatedToken: Authentication =
          userJwtAuthenticationProvider.authenticate(authentication)
      logger.debug("Storing UserJwtAuthentication to SecurityContext: $authenticatedToken")
      SecurityContextHolder.getContext().authentication = authenticatedToken
    } catch (e: AuthenticationException) {
      logger.debug("User JWT authentication failed", e)
    }

    chain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return SecurityContextHolder.getContext().authentication != null
  }
}
