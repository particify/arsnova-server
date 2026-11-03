/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

/** Has no principal: the token belongs to no account. Its only role is the reusable challenge's. */
class ReusableChallengeAuthentication(private val token: String) :
    AbstractAuthenticationToken(
        listOf(SimpleGrantedAuthority("ROLE_$CHALLENGE_SOLVED_REUSABLE_ROLE"))) {
  init {
    isAuthenticated = true
  }

  override fun getCredentials() = token

  override fun getPrincipal() = null
}
