/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class UserMutationController(
    private val userService: UserServiceImpl,
    private val localUserService: LocalUserServiceImpl,
) {
  @MutationMapping
  fun claimUnverifiedUser(
      @Argument mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): User {
    return localUserService.claimUnverifiedUser(user, mailAddress, password, locale)
  }

  @MutationMapping
  fun updateUserMailAddress(
      @Argument mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): User {
    return localUserService.initiateMailVerification(user, mailAddress, password, locale)
  }

  @MutationMapping
  fun verifyUserMailAddress(
      @Argument verificationCode: String,
      @AuthenticationPrincipal user: User
  ): User {
    return localUserService.completeMailVerification(user, verificationCode.toInt())
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun verifyUserMailAddressUnauthenticated(
      @Argument verificationCode: String,
      @Argument userId: UUID,
      @Argument password: String?
  ): User {
    val user = userService.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
    return localUserService.completeMailVerification(user, verificationCode.toInt(), password)
  }

  @MutationMapping
  fun updateUserPassword(
      @Argument oldPassword: String,
      @Argument newPassword: String,
      @AuthenticationPrincipal user: User
  ): User {
    return localUserService.updatePassword(user, oldPassword, newPassword)
  }

  @MutationMapping
  fun updateUserDetails(
      @Argument input: UpdateUserDetailsInput,
      @AuthenticationPrincipal user: User
  ): User {
    user.givenName = input.givenName
    user.surname = input.surname
    return userService.save(user)
  }

  @MutationMapping
  fun updateUserLanguage(
      @Argument @LanguageIso639 languageCode: String,
      @AuthenticationPrincipal user: User
  ): User {
    user.language = languageCode
    return userService.save(user)
  }

  @MutationMapping
  fun deleteUser(@AuthenticationPrincipal user: User): UUID {
    return userService.markAccountForDeletion(user).id!!
  }

  @MutationMapping
  fun updateUserUiSettings(
      @Argument input: MutableMap<String, Any>,
      @AuthenticationPrincipal user: User
  ): Map<String, Any> {
    user.uiSettings.putAll(input)
    return userService.save(user).uiSettings
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun requestUserPasswordReset(@Argument mailAddress: String, locale: Locale): Boolean {
    val user = userService.findByMailAddress(mailAddress) ?: throw UserNotFoundException()
    localUserService.initiatePasswordReset(user, locale)
    // Returning the user would leak information to a third party
    return true
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun resetUserPassword(
      @Argument mailAddress: String,
      @Argument password: String,
      @Argument verificationCode: String
  ): User {
    val user = userService.findByMailAddress(mailAddress) ?: throw UserNotFoundException()
    return localUserService.completePasswordReset(user, password, verificationCode.toInt())
  }

  @MutationMapping
  fun resendVerificationMail(@AuthenticationPrincipal user: User, locale: Locale): Boolean {
    return localUserService.restartVerification(user, locale)
  }
}
