/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.api

import jakarta.servlet.http.HttpServletResponse
import net.particify.arsnova.core4.system.security.JwtUtils
import net.particify.arsnova.core4.system.security.RefreshCookieComponent
import net.particify.arsnova.core4.system.security.RefreshJwtAuthentication
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@PreAuthorize("hasRole('USER')")
class AuthenticationHttpController(
    private val authenticationManager: AuthenticationManager,
    private val jwtUtils: JwtUtils,
    private val userService: UserService,
    private val refreshCookieComponent: RefreshCookieComponent
) {

  @PostMapping("/refresh")
  @PreAuthorize("hasRole('REFRESH')")
  fun refreshAuthentication(
      @AuthenticationPrincipal user: User,
      authentication: Authentication,
      response: HttpServletResponse
  ): AuthenticationWrapper {
    if (authentication !is RefreshJwtAuthentication)
        throw AccessDeniedException("Refresh not allowed for authentication")
    val subject = authentication.principal!!.id.toString()
    val accessToken = jwtUtils.encodeJwt(subject, user.roles.map { it.name!! })
    refreshCookieComponent.add(subject, user.tokenVersion!!, response)
    return AuthenticationWrapper(accessToken)
  }

  @PostMapping("/login")
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun login(
      @RequestBody loginInput: LoginInput,
      response: HttpServletResponse
  ): AuthenticationWrapper {
    val passwordToken =
        UsernamePasswordAuthenticationToken(loginInput.username.lowercase(), loginInput.password)
    val authenticatedToken = authenticationManager.authenticate(passwordToken)
    val user = authenticatedToken.principal as User
    val subject = user.id.toString()
    val accessToken = jwtUtils.encodeJwt(subject, user.roles.map { it.name!! })
    refreshCookieComponent.add(subject, user.tokenVersion!!, response)
    return AuthenticationWrapper(accessToken)
  }

  @PostMapping("/logout")
  @PreAuthorize("permitAll")
  fun logout(response: HttpServletResponse) {
    refreshCookieComponent.remove(response)
  }

  @PostMapping("/guest-account")
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun createGuestAccount(response: HttpServletResponse): AuthenticationWrapper {
    val user = userService.createAccount()
    val subject = user.id.toString()
    val accessToken = jwtUtils.encodeJwt(subject, user.roles.map { it.name!! })
    refreshCookieComponent.add(subject, user.tokenVersion!!, response)
    return AuthenticationWrapper(accessToken)
  }
}
