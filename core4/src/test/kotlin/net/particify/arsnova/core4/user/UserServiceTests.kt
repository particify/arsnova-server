/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
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

  @Test
  fun shouldDeleteMarkedUserAfter7Days() {
    val user = userDetailsService.createAccount()
    user.username = "Delete Me"
    user.deletedAt = Instant.now().minus(7, ChronoUnit.DAYS)
    userRepository.save(user)
    userDetailsService.deleteMarkedUsers()
    val retrievedUser = userDetailsService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser)
  }

  @Test
  fun shouldNotDeleteMarkedUserBefore7Days() {
    val user = userDetailsService.createAccount()
    user.username = "Do Not Delete Me"
    user.deletedAt = Instant.now().minus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES)
    userRepository.save(user)
    userDetailsService.deleteMarkedUsers()
    val retrievedUser = userDetailsService.loadUserById(user.id!!)
    Assertions.assertNotNull(retrievedUser)
  }
}
