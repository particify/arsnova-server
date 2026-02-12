/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val REFRESH_URI = "/auth/refresh"

@Component
class RefreshAuthenticationFilter(
    private val authenticationProvider: RefreshJwtAuthenticationProvider,
    private val jwtUtils: JwtUtils,
    servletContext: ServletContext
) : OncePerRequestFilter() {
  private val contextPath = servletContext.contextPath

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      chain: FilterChain
  ) {
    val header = request.getHeader(HttpHeaders.AUTHORIZATION)
    val token =
        if (header != null) jwtUtils.extractJwtString(header)
        else request.cookies?.find { it.name == REFRESH_TOKEN_COOKIE }?.value
    if (token == null) {
      return chain.doFilter(request, response)
    }

    logger.trace("Token refresh requested.")
    val authentication = RefreshJwtAuthentication(token)
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
