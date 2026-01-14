/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.ServletContext
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import org.springframework.stereotype.Component

/** Name for cookie to store the refresh token. Uses a cookie prefix for additional security. */
const val REFRESH_TOKEN_COOKIE = "__HTTP_ARS_RT"
private const val REFRESH_MAX_AGE = 3600L * 24 * 30 * 6
private const val SAME_SITE_COOKIE_ATTRIBUTE = "SameSite"
private const val REFRESH_COOKIE_POLICY = "Strict"

@Component
class RefreshCookieComponent(private val jwtUtils: JwtUtils, servletContext: ServletContext) {
  private val contextPath = servletContext.contextPath

  fun add(subject: String, version: Int, response: HttpServletResponse) {
    val expirationTime = Instant.now().plusSeconds(REFRESH_MAX_AGE)
    val refreshToken =
        jwtUtils.encodeJwt(
            subject, listOf(REFRESH_ROLE), mapOf("version" to version), expirationTime)
    val cookie = Cookie(REFRESH_TOKEN_COOKIE, refreshToken)
    cookie.path = contextPath
    cookie.isHttpOnly = true
    cookie.secure = true
    cookie.maxAge = expirationTime.epochSecond.toInt()
    cookie.setAttribute(SAME_SITE_COOKIE_ATTRIBUTE, REFRESH_COOKIE_POLICY)
    response.addCookie(cookie)
  }

  fun remove(response: HttpServletResponse) {
    val cookie = Cookie(REFRESH_TOKEN_COOKIE, null)
    cookie.path = contextPath
    cookie.isHttpOnly = true
    cookie.secure = true
    cookie.maxAge = 0
    cookie.setAttribute(SAME_SITE_COOKIE_ATTRIBUTE, REFRESH_COOKIE_POLICY)
    response.addCookie(cookie)
  }
}
