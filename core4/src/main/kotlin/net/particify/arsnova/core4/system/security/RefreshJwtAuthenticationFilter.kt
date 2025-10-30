/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/** Name for cookie to store the refresh token. Uses a cookie prefix for additional security. */
const val REFRESH_TOKEN_COOKIE = "__HTTP_ARS_RT"
const val REFRESH_AUTHORITY = "REFRESH"
private const val REFRESH_URI = "/auth/refresh"

@Component
class RefreshAuthenticationFilter(
    private val authenticationProvider: RefreshJwtAuthenticationProvider,
    servletContext: ServletContext
) : OncePerRequestFilter() {
  private val contextPath = servletContext.contextPath

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      chain: FilterChain
  ) {
    val cookie = request.cookies?.find { it.name == REFRESH_TOKEN_COOKIE }
    if (cookie == null) return chain.doFilter(request, response)

    logger.trace("Token refresh requested.")
    val authentication = RefreshJwtAuthentication(cookie.value)
    try {
      val authenticatedToken: Authentication = authenticationProvider.authenticate(authentication)
      logger.debug("Storing RefreshJwtAuthentication to SecurityContext: $authenticatedToken")
      SecurityContextHolder.getContext().authentication = authenticatedToken
    } catch (e: AuthenticationException) {
      logger.debug("Refresh JWT authentication failed", e)
    }

    chain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return request.requestURI != contextPath + REFRESH_URI ||
        SecurityContextHolder.getContext().authentication != null
  }
}
