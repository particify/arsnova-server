/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import net.particify.arsnova.core4.common.web.htmlErrorPage
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler

private const val CLOSE_SCRIPT = "window.opener ? window.close() : location.assign('/login')"

private const val SUMMARY = "Login via single sign-on failed."

private val logger = LoggerFactory.getLogger(ExternalLoginFailureHandler::class.java)

/**
 * Answers a failed single sign-on login itself instead of redirecting: the browser arrives from the
 * identity provider, often in a popup, so there is no frontend to hand the failure to.
 *
 * Only the protocol's error code is shown, and only it and the cause's class are logged, never the
 * description. That carries the message of whatever failed, including persistence errors while the
 * account is provisioned, which quote the conflicting values.
 */
abstract class ExternalLoginFailureHandler(private val protocol: String) :
    AuthenticationFailureHandler {
  protected abstract fun errorCode(exception: AuthenticationException): String?

  /** Only a configured registration is named, so the path cannot put arbitrary text in the log. */
  protected abstract fun registrationId(request: HttpServletRequest): UUID?

  override fun onAuthenticationFailure(
      request: HttpServletRequest,
      response: HttpServletResponse,
      exception: AuthenticationException
  ) {
    val errorCode = errorCode(exception)
    val cause = exception.cause?.let { " (caused by ${it.javaClass.simpleName})" }.orEmpty()
    logger.warn(
        "{} login via registration {} failed: {}{}",
        protocol,
        registrationId(request) ?: "(unresolved)",
        errorCode ?: exception.javaClass.simpleName,
        cause)
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.contentType = MediaType.TEXT_HTML_VALUE
    response.characterEncoding = Charsets.UTF_8.name()
    response.writer.write(htmlErrorPage(HttpStatus.UNAUTHORIZED, SUMMARY, errorCode, CLOSE_SCRIPT))
  }
}
