/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import net.particify.arsnova.core4.user.AuthenticatedUser
import net.particify.arsnova.core4.user.User
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class RefreshJwtAuthentication(
    private val token: String,
    private var principal: AuthenticatedUser? = null,
    /**
     * The account itself, which a refresh updates. It is the instance whose token version was
     * verified, so a concurrent invalidation fails the refresh rather than being renewed past.
     */
    val user: User? = null,
    grantedAuthorities: Set<GrantedAuthority> = emptySet(),
    val sessionLifetime: RefreshSessionLifetime = RefreshSessionLifetime(),
) : AbstractAuthenticationToken(grantedAuthorities) {
  init {
    isAuthenticated = grantedAuthorities.isNotEmpty()
  }

  override fun getCredentials() = token

  override fun getPrincipal() = principal
}
