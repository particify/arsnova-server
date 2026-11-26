/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.particify.arsnova.core4.user.AuthenticatedUser
import net.particify.arsnova.core4.user.User
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

/**
 * Replaces the account which HTTP Basic authentication leaves as the principal with the
 * [AuthenticatedUser] that controllers expect, granting the authorities a JWT would. The password
 * login shares the authentication provider and needs the account, so the provider cannot map it.
 *
 * Not a bean, so it is not registered as a servlet filter outside the chain enabling HTTP Basic.
 */
class HttpBasicPrincipalFilter(private val userAuthorities: UserAuthorities) :
    SecurityContextSavingFilter() {
  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      chain: FilterChain
  ) {
    val authentication = securityContextHolderStrategy.context.authentication
    val user = authentication?.principal
    if (authentication is UsernamePasswordAuthenticationToken && user is User) {
      val principal = AuthenticatedUser.of(user)
      val mapped =
          UsernamePasswordAuthenticationToken.authenticated(
              principal, null, userAuthorities.of(principal))
      mapped.details = authentication.details
      saveAuthentication(mapped, request, response)
    }
    chain.doFilter(request, response)
  }
}
