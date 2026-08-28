/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.ExternalLogin
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class UserServiceTests {
  @Autowired lateinit var userDetailsService: UserServiceImpl
  @Autowired lateinit var userRepository: UserRepository

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
    Assertions.assertEquals(userId, user.id)
    Assertions.assertEquals(username, user.username)
  }

  @Test
  fun shouldCreateUser() {
    val user = userDetailsService.createAccount()
    val retrievedUser = userDetailsService.loadUserById(user.id!!)
    Assertions.assertNotNull(retrievedUser)
  }

  @Test
  fun shouldMarkUserForDeletion() {
    val user = userDetailsService.createAccount()
    userDetailsService.markAccountForDeletion(user)
    Assertions.assertNotNull(user.deletedAt)
  }

  /**
   * Each external login is only valid for the combination of its provider and external ID. A user
   * holding logins at two providers must not be resolvable by mixing one provider with the other
   * provider's external ID.
   */
  @Test
  fun shouldNotFindUserForExternalLoginOfDifferentProvider() {
    val firstProviderId = UUID.fromString("11111111-1111-4111-8111-111111111111")
    val secondProviderId = UUID.fromString("22222222-2222-4222-8222-222222222222")
    var user = userDetailsService.createAccount()
    user =
        userDetailsService.createForExternalLogin(
            user, ExternalLogin(providerId = firstProviderId, externalId = "alice"))
    user =
        userDetailsService.createForExternalLogin(
            user, ExternalLogin(providerId = secondProviderId, externalId = "bob"))

    Assertions.assertEquals(
        user.id, userDetailsService.loadUserByProviderIdAndExternalId(firstProviderId, "alice")?.id)
    Assertions.assertEquals(
        user.id, userDetailsService.loadUserByProviderIdAndExternalId(secondProviderId, "bob")?.id)
    Assertions.assertNull(
        userDetailsService.loadUserByProviderIdAndExternalId(firstProviderId, "bob"))
  }
}
