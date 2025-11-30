/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.user.LocalUserService
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Mutation")
class AdminUserMutationController(
    private val userService: UserServiceImpl,
    private val localUserService: LocalUserService,
) {
  @MutationMapping
  fun adminVerifyUserById(@Argument id: UUID): User {
    val user = userService.findByIdOrNull(id) ?: throw UserNotFoundException(id)
    return localUserService.verifyUser(user)
  }

  @MutationMapping
  fun adminCreateUser(
      @Argument mailAddress: String,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): User {
    return localUserService.inviteUser(
        user, mailAddress, "admin-invitation-verification", mapOf(), locale)
  }

  @MutationMapping
  fun adminDeleteUserById(@Argument id: UUID): UUID {
    userService.deleteById(id)
    return id
  }
}
