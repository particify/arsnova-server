/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.LocalUserService
import net.particify.arsnova.core4.user.internal.UserRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Mutation")
class UserMutationController(
    private val localUserService: LocalUserService,
    private val userRepository: UserRepository,
) {
  @MutationMapping
  fun claimUnverifiedUser(
      @Argument mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal user: User
  ): Boolean {
    return localUserService.claimUnverifiedUser(user, mailAddress, password)
  }

  @MutationMapping
  fun updateUserMailAddress(
      @Argument mailAddress: String,
      @AuthenticationPrincipal user: User
  ): Boolean {
    localUserService.initiateMailVerification(user, mailAddress)
    return true
  }

  @MutationMapping
  fun verifyUserMailAddress(
      @Argument verificationCode: String,
      @AuthenticationPrincipal user: User
  ): Boolean {
    return localUserService.completeMailVerification(user, verificationCode.toInt())
  }

  @MutationMapping
  fun updateUserPassword(
      @Argument oldPassword: String,
      @Argument newPassword: String,
      @AuthenticationPrincipal user: User
  ): Boolean {
    return localUserService.updatePassword(user, oldPassword, newPassword)
  }

  @MutationMapping
  fun updateUserDetails(
      @Argument input: UpdateUserDetailsInput,
      @AuthenticationPrincipal user: User
  ): Boolean {
    user.givenName = input.givenName
    user.surname = input.surname
    userRepository.save(user)
    return true
  }
}
