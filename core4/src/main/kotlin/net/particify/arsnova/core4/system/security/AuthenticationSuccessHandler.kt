/* Copyright 2019-2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.ServletContext
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.particify.arsnova.core4.user.User
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class AuthenticationSuccessHandler(
    private val servletContext: ServletContext,
    private val jwtUtils: JwtUtils
) : SimpleUrlAuthenticationSuccessHandler() {
  companion object {
    const val AUTH_COOKIE_NAME: String = "auth"
    const val URL_ATTRIBUTE: String = "ars-login-success-url"
  }

  override fun determineTargetUrl(
      request: HttpServletRequest,
      response: HttpServletResponse?
  ): String? {
    val session = request.getSession(false)
    val url = session.getAttribute(URL_ATTRIBUTE) as String?
    session.removeAttribute(URL_ATTRIBUTE)

    return url
  }

  override fun onAuthenticationSuccess(
      request: HttpServletRequest,
      response: HttpServletResponse,
      authentication: Authentication
  ) {
    val session = request.getSession(false)
    if (session == null || session.getAttribute(URL_ATTRIBUTE) == null) {
      val user = authentication.principal as User
      val token = jwtUtils.encodeJwt(user.id.toString(), user.roles.map { it.name!! })
      val cookie = Cookie(AUTH_COOKIE_NAME, token)
      cookie.path = servletContext.contextPath
      cookie.secure = request.isSecure
      cookie.isHttpOnly = true
      response.addCookie(cookie)
      response.contentType = MediaType.TEXT_HTML_VALUE
      response.writer.println(
          "<!DOCTYPE html><script>if (window.opener) window.close(); else location.href='/login/complete'</script>")

      return
    }
    super.onAuthenticationSuccess(request, response, authentication)
  }
}
