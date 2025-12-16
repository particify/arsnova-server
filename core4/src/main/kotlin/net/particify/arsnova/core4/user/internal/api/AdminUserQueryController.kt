/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import com.querydsl.core.BooleanBuilder
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.user.QUser
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class AdminUserQueryController(private val userService: UserServiceImpl) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 10
  }

  @QueryMapping
  fun adminUserById(@Argument id: UUID): User {
    return this.userService.loadUserById(id) ?: throw UserNotFoundException(id)
  }

  @QueryMapping
  fun adminUsers(
      @Argument search: String?,
      @Argument exactSearchMode: Boolean?,
      subrange: ScrollSubrange
  ): Window<User> {
    val queryBuilder = BooleanBuilder()
    val queryFilter = BooleanBuilder()
    val qUser = QUser.user
    if (exactSearchMode == true) {
      queryFilter.or(qUser.username.eq(search))
      queryFilter.or(qUser.mailAddress.eq(search))
      queryFilter.or(qUser.unverifiedMailAddress.eq(search))
      queryBuilder.and(queryFilter)
    } else {
      if (search != null) {
        queryFilter.or(qUser.username.containsIgnoreCase(search))
        queryFilter.or(qUser.mailAddress.containsIgnoreCase(search))
        queryFilter.or(qUser.unverifiedMailAddress.containsIgnoreCase(search))
        queryBuilder.or(queryFilter)
      } else {
        queryBuilder.and(qUser.username.isNotNull.or(qUser.unverifiedMailAddress.isNotNull))
      }
    }
    return userService.findBy(queryBuilder) { q ->
      q.sortBy(Sort.by("auditMetadata.createdAt").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @SchemaMapping(typeName = "AdminUser")
  fun verificationExpiresAt(user: User): OffsetDateTime? {
    return user.verificationExpiresAt?.atOffset(ZoneOffset.UTC)
  }
}
