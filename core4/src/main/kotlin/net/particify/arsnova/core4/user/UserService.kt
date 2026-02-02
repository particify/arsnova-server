/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.springframework.security.core.userdetails.UserDetailsService

interface UserService : UserDetailsService {
  fun loadUserById(id: UUID): User?

  fun markAnnouncementsReadForUserId(id: UUID)

  fun findRoleByName(name: String): Role

  fun createAccount(): User

  fun updateLastActivityAt(user: User): User
}
