/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import java.util.regex.Pattern
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

private val ID_PATTERN = Pattern.compile("challenge-(.*)")

@Component
class ChallengeAuthenticationProvider(private val jwtUtils: JwtUtils) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication? {
    val token = authentication.credentials as String
    try {
      val jwt = jwtUtils.decodeJwt(authentication.credentials as String)
      val roles = jwt.claims["roles"]
      if (roles !is List<*> || !roles.contains(CHALLENGE_SOLVED_ROLE)) {
        return null
      }
      val matcher = ID_PATTERN.matcher(jwt.subject)
      val id =
          if (matcher.matches()) matcher.group(1)
          else throw BadCredentialsException("Invalid ID pattern for subject")
      return ChallengeJwtAuthentication(
          token, UUID.fromString(id), setOf(SimpleGrantedAuthority(CHALLENGE_SOLVED_ROLE)))
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
