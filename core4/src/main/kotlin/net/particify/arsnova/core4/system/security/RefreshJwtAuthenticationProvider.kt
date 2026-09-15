/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
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
      return authenticationFor(token, jwtUtils.decodeJwt(token))
    } catch (e: JwtException) {
      throw BadCredentialsException("Invalid JWT", e)
    } catch (e: IllegalArgumentException) {
      throw BadCredentialsException("Invalid JWT", e)
    }
  }

  /**
   * Rejects a decoded token which does not authorize a refresh. Reading the subject can fail with
   * an [IllegalArgumentException], which is why the caller runs this within its own `try`.
   */
  @OptIn(ExperimentalUuidApi::class)
  private fun authenticationFor(token: String, jwt: Jwt): RefreshJwtAuthentication {
    val subject = requireNotNull(jwt.subject)
    val roles = jwt.claims["roles"] as? Collection<*> ?: emptyList<String>()
    val isLegacy = roles.contains(LEGACY_GUEST_ROLE)
    if (!roles.contains(REFRESH_ROLE) && !isLegacy)
        throw BadCredentialsException("Not a refresh token")
    val userId = if (isLegacy) Uuid.parseHex(subject).toJavaUuid() else UUID.fromString(subject)
    val user =
        userService.loadUserById(userId) ?: throw BadCredentialsException("User for JWT not found.")
    val version = (jwt.claims["version"] as? Long)?.toInt() ?: 1
    if (version != user.tokenVersion) throw BadCredentialsException("Invalid token version")
    val sessionLifetime = RefreshSessionLifetime.fromClaims(jwt.claims)
    // The decoder tolerates some clock skew, so a token can outlive the session by a moment.
    if (sessionLifetime.hasEnded()) throw BadCredentialsException("Session has ended")
    val updatedUser = if (isLegacy) userService.invalidateToken(user) else user
    return RefreshJwtAuthentication(
        token, updatedUser, setOf(SimpleGrantedAuthority("ROLE_$REFRESH_ROLE")), sessionLifetime)
  }

  override fun supports(authentication: Class<*>): Boolean {
    return RefreshJwtAuthentication::class.java.isAssignableFrom(authentication)
  }
}
