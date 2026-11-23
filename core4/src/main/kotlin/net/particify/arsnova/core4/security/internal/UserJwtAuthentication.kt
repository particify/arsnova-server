/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.time.Instant
import net.particify.arsnova.core4.user.AuthenticatedUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class UserJwtAuthentication(
    private val token: String,
    private var principal: AuthenticatedUser? = null,
    grantedAuthorities: Set<GrantedAuthority> = emptySet(),
    val expiresAt: Instant? = null
) : AbstractAuthenticationToken(grantedAuthorities) {
  init {
    isAuthenticated = grantedAuthorities.isNotEmpty()
  }

  override fun getCredentials() = token

  override fun getPrincipal() = principal
}
