/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class UserJwtAuthenticationProvider(
    private var jwtUtils: JwtUtils,
    private val userService: UserService
) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication {
    val token = authentication.credentials as String
    try {
      val jwt = jwtUtils.decodeJwt(authentication.credentials as String)
      val user =
          userService.loadUserById(UUID.fromString(jwt.subject))
              ?: throw BadCredentialsException("User for JWT not found.")
      return UserJwtAuthentication(token, user, user.getAuthorities())
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
