/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class UserTests {
  @Autowired lateinit var userDetailsService: UserServiceImpl

  @Test
  fun shouldFindCorrectUserForId() {
    val userId = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
    val user = userDetailsService.loadUserById(userId)
    Assertions.assertNotNull(user)
    Assertions.assertEquals(userId, user!!.id)
  }

  @Test
  fun shouldFindCorrectUserForUserId() {
    val userId = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
    val username = "admin@example.com"
    val user = userDetailsService.loadUserByUsername(username)
    Assertions.assertNotNull(user)
    Assertions.assertEquals(userId, user!!.id)
    Assertions.assertEquals(username, user!!.username)
  }
}
