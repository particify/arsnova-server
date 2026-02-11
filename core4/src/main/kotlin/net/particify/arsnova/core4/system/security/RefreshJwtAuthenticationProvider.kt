/* Copyright 2025-2026 Particify GmbH
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

const val REFRESH_ROLE = "REFRESH"
const val LEGACY_GUEST_ROLE = "GUEST_USER"

@Component
class RefreshJwtAuthenticationProvider(
    private var jwtUtils: JwtUtils,
    private val userService: UserService
) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication {
    val token = authentication.credentials as String
    try {
      val jwt = jwtUtils.decodeJwt(authentication.credentials as String)
      val roles = jwt.claims["roles"] as? Collection<*> ?: emptyList<String>()
      val isLegacy = roles.contains(LEGACY_GUEST_ROLE)
      if (!roles.contains(REFRESH_ROLE) && !isLegacy)
          throw BadCredentialsException("Not a refresh token")
      val user =
          userService.loadUserById(UUID.fromString(jwt.subject))
              ?: throw BadCredentialsException("User for JWT not found.")
      val version = (jwt.claims["version"] as? Long)?.toInt() ?: 1
      if (version != user.tokenVersion) throw BadCredentialsException("Invalid token version")
      val updatedUser = if (isLegacy) userService.invalidateToken(user) else user
      return RefreshJwtAuthentication(
          token, updatedUser, setOf(SimpleGrantedAuthority("ROLE_$REFRESH_ROLE")))
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
