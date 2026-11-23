/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * The principal of a request authenticated by an access token. It holds only what authorization
 * needs, so a request which needs more of the account loads it by [id].
 *
 * @param roles the names of the account's roles
 * @param verified whether the account has a username, which only a verified mail address grants
 */
data class AuthenticatedUser(val id: UUID, val roles: Set<String>, val verified: Boolean) :
    AuthenticatedPrincipal {
  override fun getName(): String = id.toString()

  fun authorities(): Set<GrantedAuthority> =
      roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

  companion object {
    fun of(user: User) =
        AuthenticatedUser(
            checkNotNull(user.id), user.roles.mapNotNull { it.name }.toSet(), user.username != null)
  }
}
