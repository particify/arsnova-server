/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import net.particify.arsnova.core4.user.User
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class RefreshJwtAuthentication(
    private val token: String,
    private var principal: User? = null,
    grantedAuthorities: Set<GrantedAuthority?> = emptySet(),
) : AbstractAuthenticationToken(grantedAuthorities) {
  init {
    isAuthenticated = grantedAuthorities.isNotEmpty()
  }

  override fun getCredentials() = token

  override fun getPrincipal() = principal
}
