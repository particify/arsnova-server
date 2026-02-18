/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.UserBulkDeletionService
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.auditing.AuditingHandler
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class UserBulkDeletionServiceTests {
  @Autowired lateinit var userBulkDeletionService: UserBulkDeletionService
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var userRepository: UserRepository
  @Autowired lateinit var auditingHandler: AuditingHandler

  @Test
  fun shouldDeleteMarkedUserAfter7Days() {
    val user = userService.createAccount()
    user.username = "Delete Me"
    user.deletedAt = Instant.now().minus(7, ChronoUnit.DAYS)
    userRepository.save(user)
    userBulkDeletionService.deleteMarkedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser)
  }

  @Test
  fun shouldNotDeleteMarkedUserBefore7Days() {
    val user = userService.createAccount()
    user.username = "Do Not Delete Me"
    user.deletedAt = Instant.now().minus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES)
    userRepository.save(user)
    userBulkDeletionService.deleteMarkedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNotNull(retrievedUser)
  }

  @Test
  fun shouldMarkSingleVisitUserAfter90Days() {
    val createdAt = Instant.now().minus(90, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.lastActivityAt = createdAt
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNotNull(retrievedUser?.deletedAt)
  }

  @Test
  fun shouldNotMarkSingleVisitUserBefore90Days() {
    val createdAt = Instant.now().minus(89, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.lastActivityAt = createdAt
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser?.deletedAt)
  }

  @Test
  fun shouldMarkNonSingleVisitUserAfter365Days() {
    val createdAt = Instant.now().minus(365, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.lastActivityAt = createdAt.plus(1, ChronoUnit.DAYS)
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveUnverifiedUsers()
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    userBulkDeletionService.deleteInactiveVerifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser?.deletedAt)
  }

  @Test
  fun shouldNotMarkNonSingleVisitUserBefore365Days() {
    val createdAt = Instant.now().minus(364, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.lastActivityAt = createdAt.plus(1, ChronoUnit.DAYS)
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveUnverifiedUsers()
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    userBulkDeletionService.deleteInactiveVerifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser?.deletedAt)
  }

  @Test
  fun shouldMarkVerifiedUserAfter730Days() {
    val createdAt = Instant.now().minus(730, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.username = "Inactive Verified User"
    user.lastActivityAt = createdAt
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveVerifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNotNull(retrievedUser?.deletedAt)
  }

  @Test
  fun shouldNotMarkVerifiedUserBefore730Days() {
    val createdAt = Instant.now().minus(729, ChronoUnit.DAYS)
    auditingHandler.setDateTimeProvider { Optional.of(createdAt) }
    val user = userService.createAccount()
    user.username = "Non-Inactive Verified User"
    user.lastActivityAt = createdAt
    userRepository.save(user)
    userBulkDeletionService.deleteInactiveUnverifiedUsers()
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    userBulkDeletionService.deleteInactiveVerifiedUsers()
    val retrievedUser = userService.loadUserById(user.id!!)
    Assertions.assertNull(retrievedUser?.deletedAt)
  }
}
