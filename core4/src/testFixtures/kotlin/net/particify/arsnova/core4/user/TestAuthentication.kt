/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

/** Authenticates as [user] with the principal which a request authenticated by a token carries. */
fun authenticationOf(
    user: User,
    authorities: Collection<GrantedAuthority> = user.authorities
): Authentication =
    UsernamePasswordAuthenticationToken(AuthenticatedUser.of(user), null, authorities)
