/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.Limit
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface UserRepository : JpaRepository<User, UUID>, QuerydslPredicateExecutor<User> {
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  fun findByIdOrNull(id: UUID): User?

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
  fun findOneByUsername(username: String): User?

  fun findByAuditMetadataDeletedAtBefore(
      deletedAtBefore: Instant,
      scrollPosition: ScrollPosition,
      limit: Limit
  ): Window<User>

  fun findByMailAddress(mailAddress: String): User?

  fun countByUsernameIsNotNull(): Long

  fun countByUsernameIsNullAndUnverifiedMailAddressIsNotNull(): Long
}
