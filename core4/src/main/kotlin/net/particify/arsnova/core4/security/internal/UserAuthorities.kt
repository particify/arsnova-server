/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import net.particify.arsnova.core4.security.RoomCreationPolicy
import net.particify.arsnova.core4.user.AuthenticatedUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

private val ROLE_ROOM_CREATOR = SimpleGrantedAuthority("ROLE_ROOM_CREATOR")
private val ROLE_VERIFIED = SimpleGrantedAuthority("ROLE_VERIFIED")

/** The authorities a request authenticated as an account is granted. */
@Component
class UserAuthorities(private val roomCreationPolicy: RoomCreationPolicy) {
  fun of(user: AuthenticatedUser): Set<GrantedAuthority> {
    var authorities = user.authorities()
    if (user.verified) {
      authorities = authorities.plus(ROLE_VERIFIED)
    }
    if (roomCreationPolicy.mayCreateRooms(user)) {
      authorities = authorities.plus(ROLE_ROOM_CREATOR)
    }
    return authorities
  }
}
