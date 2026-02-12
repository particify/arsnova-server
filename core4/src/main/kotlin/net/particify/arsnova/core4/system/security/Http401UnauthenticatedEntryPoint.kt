/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class Http401UnauthenticatedEntryPoint : AuthenticationEntryPoint {
  override fun commence(
      request: HttpServletRequest,
      response: HttpServletResponse,
      authException: AuthenticationException
  ) {
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
  }
}
