/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class RefreshJwtAuthenticationProvider(
    private var jwtUtils: JwtUtils,
    private val userService: UserService
) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication {
    val token = authentication.credentials as String
    try {
      val jwt = jwtUtils.decodeJwt(authentication.credentials as String)
      val roles = jwt.claims["roles"]
      if (roles !is Collection<*> || !roles.contains(REFRESH_AUTHORITY))
          throw BadCredentialsException("Not a refresh token")
      val user =
          userService.loadUserById(UUID.fromString(jwt.subject))
              ?: throw BadCredentialsException("User for JWT not found.")
      val version = (jwt.claims["version"] as Long).toInt()
      if (version != user.tokenVersion) throw BadCredentialsException("Invalid token version")
      return RefreshJwtAuthentication(token, user, setOf(SimpleGrantedAuthority(REFRESH_AUTHORITY)))
    } catch (e: JwtException) {
      throw BadCredentialsException("Invalid JWT", e)
    } catch (e: IllegalArgumentException) {
      throw BadCredentialsException("Invalid JWT", e)
    }
  }

  override fun supports(authentication: Class<*>): Boolean {
    return RefreshJwtAuthentication::class.java.isAssignableFrom(authentication)
  }
}
