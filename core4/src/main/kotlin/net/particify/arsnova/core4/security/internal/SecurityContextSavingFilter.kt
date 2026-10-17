/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Base for authentication filters that store their result the way Spring Security's own
 * `AuthenticationFilter` does.
 *
 * Saving to the [SecurityContextRepository] is what makes the authentication visible to later
 * dispatches of the same request, such as the ASYNC dispatch completing an asynchronous GraphQL
 * response, where `OncePerRequestFilter` subclasses do not run again.
 */
abstract class SecurityContextSavingFilter : OncePerRequestFilter() {
  var securityContextHolderStrategy: SecurityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy()
  var securityContextRepository: SecurityContextRepository =
      RequestAttributeSecurityContextRepository()

  protected fun isAuthenticated(): Boolean =
      securityContextHolderStrategy.context.authentication != null

  protected fun saveAuthentication(
      authentication: Authentication,
      request: HttpServletRequest,
      response: HttpServletResponse
  ) {
    val context = securityContextHolderStrategy.createEmptyContext()
    context.authentication = authentication
    securityContextHolderStrategy.context = context
    securityContextRepository.saveContext(context, request, response)
  }
}
