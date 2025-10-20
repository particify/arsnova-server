/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

const val CHALLENGE_SOLVED_ROLE = "CHALLENGE_SOLVED"

class ChallengeJwtAuthentication(
    private val token: String,
    private var principal: UUID? = null,
    grantedAuthorities: Set<GrantedAuthority?> = emptySet()
) : AbstractAuthenticationToken(grantedAuthorities) {
  init {
    isAuthenticated = grantedAuthorities.isNotEmpty()
  }

  override fun getCredentials() = token

  override fun getPrincipal() = principal
}
