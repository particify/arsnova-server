/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

@Component
class JwtAuthenticationFilter(private val jwtAuthenticationProvider: JwtAuthenticationProvider) :
    GenericFilterBean() {
  companion object {
    private val BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE)
  }

  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val httpServletRequest = request as HttpServletRequest
    val jwtHeader =
        httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
            ?: return chain.doFilter(request, response)

    val tokenMatcher: Matcher = BEARER_TOKEN_PATTERN.matcher(jwtHeader)
    if (!tokenMatcher.matches()) {
      logger.debug("Skipping JWT handling due to pattern mismatch.")
      return chain.doFilter(request, response)
    }
    val authentication = JwtAuthentication(tokenMatcher.group(1))

    try {
      val authenticatedToken: Authentication =
          jwtAuthenticationProvider.authenticate(authentication)
      logger.debug("Storing JWT to SecurityContext: $authenticatedToken")
      SecurityContextHolder.getContext().authentication = authenticatedToken
    } catch (e: AuthenticationException) {
      logger.debug("JWT authentication failed", e)
    }

    chain.doFilter(request, response)
  }
}
