/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import net.particify.arsnova.core4.common.web.htmlErrorPage
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

private const val CLOSE_SCRIPT = "window.opener ? window.close() : location.assign('/login')"

private const val REGISTRATION_ID_VARIABLE = "registrationId"

private const val SUMMARY = "Login via single sign-on failed."

private val logger = LoggerFactory.getLogger(Saml2AuthenticationFailureHandler::class.java)

/**
 * Answers a rejected assertion itself instead of redirecting: the browser arrives by posting the
 * identity provider's form, often in a popup, so there is no frontend to hand the failure to.
 *
 * Only the `Saml2Error` code is shown, and only it and the cause's class are logged, never the
 * description. That carries the message of whatever failed, including persistence errors while the
 * account is provisioned, which quote the conflicting values. The whole exception is logged by
 * `Saml2WebSsoAuthenticationFilter` at TRACE.
 */
class Saml2AuthenticationFailureHandler(saml2Properties: ExtendedSaml2RelyingPartyProperties) :
    AuthenticationFailureHandler {
  private val registrationIds = saml2Properties.registration.keys
  private val configuredPaths =
      Saml2AssertionConsumerServices(saml2Properties).configuredPaths().map { (id, path) ->
        PathPatternRequestMatcher.pathPattern(path) to id
      }
  private val defaultPath =
      PathPatternRequestMatcher.pathPattern(
          DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION.removePrefix("{baseUrl}"))

  override fun onAuthenticationFailure(
      request: HttpServletRequest,
      response: HttpServletResponse,
      exception: AuthenticationException
  ) {
    val errorCode = (exception as? Saml2AuthenticationException)?.saml2Error?.errorCode
    val cause = exception.cause?.let { " (caused by ${it.javaClass.simpleName})" }.orEmpty()
    logger.warn(
        "SAML login via registration {} failed: {}{}",
        registrationId(request) ?: "(unresolved)",
        errorCode ?: exception.javaClass.simpleName,
        cause)
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.contentType = MediaType.TEXT_HTML_VALUE
    response.characterEncoding = Charsets.UTF_8.name()
    response.writer.write(htmlErrorPage(HttpStatus.UNAUTHORIZED, SUMMARY, errorCode, CLOSE_SCRIPT))
  }

  /** Only a configured registration is named, so the path cannot put arbitrary text in the log. */
  private fun registrationId(request: HttpServletRequest): UUID? =
      configuredPaths.firstOrNull { (matcher) -> matcher.matches(request) }?.second
          ?: defaultPath
              .matcher(request)
              .variables[REGISTRATION_ID_VARIABLE]
              ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
              ?.takeIf { it in registrationIds }
}
