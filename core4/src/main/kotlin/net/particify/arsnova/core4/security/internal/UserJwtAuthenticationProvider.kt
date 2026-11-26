/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.util.UUID
import net.particify.arsnova.core4.user.AuthenticatedUser
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class UserJwtAuthenticationProvider(
    private var jwtUtils: JwtUtils,
    private val userAuthorities: UserAuthorities,
    private val userService: UserService
) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication {
    val token = authentication.credentials as String
    try {
      val jwt = jwtUtils.decodeJwt(authentication.credentials as String)
      val user =
          userService.loadUserById(UUID.fromString(jwt.subject))?.let(AuthenticatedUser::of)
              ?: throw BadCredentialsException("User for JWT not found.")
      val authorities = userAuthorities.of(user).plus(authentication.authorities)
      return UserJwtAuthentication(token, user, authorities, jwt.expiresAt)
    } catch (e: JwtException) {
      throw BadCredentialsException("Invalid JWT", e)
    } catch (e: IllegalArgumentException) {
      throw BadCredentialsException("Invalid JWT", e)
    }
  }

  override fun supports(authentication: Class<*>): Boolean {
    return UserJwtAuthentication::class.java.isAssignableFrom(authentication)
  }
}
