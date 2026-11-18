/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

class UserServiceImplTests {
  private val userRepository = Mockito.mock(UserRepository::class.java)
  private val userService =
      UserServiceImpl(
          userRepository,
          Mockito.mock(RoleRepository::class.java),
          Mockito.mock(ApplicationEventPublisher::class.java))

  /** Saving a new user merges it, which returns a managed copy rather than the instance passed. */
  @Test
  fun shouldReturnPersistedGhostUserWhenCreatingIt() {
    Mockito.`when`(userRepository.findOneByUsername(Mockito.anyString())).thenReturn(null)
    val persistedUser = User(id = UUID.randomUUID())
    Mockito.`when`(userRepository.save(Mockito.any(User::class.java))).thenReturn(persistedUser)
    Assertions.assertEquals(persistedUser.id, userService.getOrCreateGhostUser().id)
  }
}
