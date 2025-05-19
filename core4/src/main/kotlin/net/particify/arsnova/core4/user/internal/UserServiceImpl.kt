/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.stereotype.Component

@Component
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
  override fun loadUserByUsername(username: String): User? {
    return userRepository.findOneByUsername(username)
  }

  override fun loadUserById(id: UUID): User? {
    return userRepository.findByIdOrNull(id)
  }
}
