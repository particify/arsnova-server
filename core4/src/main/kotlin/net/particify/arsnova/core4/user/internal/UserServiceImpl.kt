/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import net.particify.arsnova.core4.user.QUser
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserDeletedEvent
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Limit
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.support.WindowIterator
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DELETE_AFTER_DAYS = 7L
private const val DELETE_BATCH_SIZE = 10

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val eventPublisher: ApplicationEventPublisher
) : UserService {
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
    return userRepository.save(user)
  }

  @Transactional
  fun createForExternalLogin(user: User, externalLogin: ExternalLogin): User {
    externalLogin.user = user
    externalLogin.lastLoginAt = Instant.now()
    user.roles += userRole
    user.externalLogins += externalLogin
    return userRepository.save(user)
  }

  fun markAccountForDeletion(user: User): User {
    user.enabled = false
    user.auditMetadata.deletedAt = Instant.now()
    user.auditMetadata.deletedBy = user.id
    return userRepository.save(user)
  }

  @Transactional
  fun deleteMarkedUsers() {
    val users =
        WindowIterator.of {
              userRepository.findByAuditMetadataDeletedAtBefore(
                  Instant.now().minus(DELETE_AFTER_DAYS, ChronoUnit.DAYS),
                  it,
                  Limit.of(DELETE_BATCH_SIZE))
            }
            .startingAt(ScrollPosition.offset())
    users.forEachRemaining {
      eventPublisher.publishEvent(UserDeletedEvent(it.id!!))
      it.clearForSoftDelete()
      it.enabled = false
      it.roles.clear()
      userRepository.saveAndFlush(it)
      userRepository.delete(it)
    }
  }
}
