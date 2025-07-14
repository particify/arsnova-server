/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.stereotype.Component

@Component
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : UserService {
  override fun loadUserByUsername(username: String): User? {
    return userRepository.findOneByUsername(username)
  }

  override fun loadUserById(id: UUID): User? {
    return userRepository.findByIdOrNull(id)
  }

  override fun markAnnouncementsReadForUserId(id: UUID) {
    val user = userRepository.findByIdOrNull(id) ?: error("User not found.")
    user.announcementsReadAt = Instant.now()
    userRepository.save(user)
  }

  override fun findRoleByName(name: String): Role {
    return roleRepository.findByName(name)
  }
}
