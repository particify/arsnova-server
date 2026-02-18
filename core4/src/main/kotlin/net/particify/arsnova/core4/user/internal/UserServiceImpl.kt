/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import net.particify.arsnova.core4.user.QUser
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.event.UserCreatedEvent
import net.particify.arsnova.core4.user.event.UserMarkedForDeletionEvent
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val eventPublisher: ApplicationEventPublisher
) : UserService, UserRepository by userRepository {
  private val userRole = roleRepository.findByName("USER")

  override fun loadUserByUsername(username: String): User {
    return userRepository.findOneByUsername(username)
        ?: throw UsernameNotFoundException("Username $username not found.")
  }

  override fun loadUserById(id: UUID): User? {
    return userRepository.findByIdOrNull(id)
  }

  fun loadUserByProviderIdAndExternalId(providerId: UUID, externalId: String): User? {
    val q =
        QUser.user.externalLogins
            .any()
            .providerId
            .eq(providerId)
            .and(QUser.user.externalLogins.any().externalId.eq(externalId))
    return userRepository.findOne(q).getOrNull()
  }

  override fun markAnnouncementsReadForUserId(id: UUID) {
    val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException(id)
    user.announcementsReadAt = Instant.now()
    userRepository.save(user)
  }

  override fun findRoleByName(name: String): Role {
    return roleRepository.findByName(name)
  }

  override fun createAccount(): User {
    val user = User(roles = mutableListOf(userRole))
    val persistedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserCreatedEvent(persistedUser.id!!))
    return persistedUser
  }

  @Transactional
  fun createForExternalLogin(user: User, externalLogin: ExternalLogin): User {
    externalLogin.user = user
    externalLogin.lastLoginAt = Instant.now()
    user.roles += userRole
    user.externalLogins += externalLogin
    return userRepository.save(user)
  }

  @Transactional
  fun markAccountForDeletion(user: User): User {
    user.enabled = false
    user.deletedAt = Instant.now()
    val updatedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserMarkedForDeletionEvent(updatedUser.id!!))
    return updatedUser
  }

  override fun updateLastActivityAt(user: User): User {
    user.lastActivityAt = Instant.now()
    return userRepository.save(user)
  }

  override fun invalidateToken(user: User): User {
    user.tokenVersion = user.tokenVersion!! + 1
    return userRepository.save(user)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<User>) = userRepository.deleteInBatch(entities)
}
