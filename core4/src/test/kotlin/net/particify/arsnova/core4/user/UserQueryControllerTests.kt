/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.api.UserQueryController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
@WithMockUser
class UserQueryControllerTests {
  @Autowired lateinit var userQueryController: UserQueryController

  private val localAccountUser =
      User(
          id = UUID.nameUUIDFromBytes("Local Account User".toByteArray()),
          password = "{noop}password")
  private val directoryUser = User(id = UUID.nameUUIDFromBytes("Directory User".toByteArray()))

  @Test
  fun shouldResolveLocalPasswordSet() {
    Assertions.assertEquals(
        true, userQueryController.localPasswordSet(localAccountUser, localAccountUser))
  }

  @Test
  fun shouldResolveLocalPasswordSetWithoutPassword() {
    Assertions.assertEquals(
        false, userQueryController.localPasswordSet(directoryUser, directoryUser))
  }

  /** The lookup by display ID returns the same type, which must not disclose this. */
  @Test
  fun shouldNotResolveLocalPasswordSetOfOtherUser() {
    Assertions.assertNull(userQueryController.localPasswordSet(directoryUser, localAccountUser))
  }

  @Test
  fun shouldNotResolveLocalPasswordSetWithoutPrincipal() {
    Assertions.assertNull(userQueryController.localPasswordSet(localAccountUser, null))
  }
}
