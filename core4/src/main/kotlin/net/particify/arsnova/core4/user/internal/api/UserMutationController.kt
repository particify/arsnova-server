/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Mutation")
class UserMutationController(
    private val userService: UserServiceImpl,
    private val localUserService: LocalUserServiceImpl,
    private val userRepository: UserRepository,
) {
  @MutationMapping
  fun claimUnverifiedUser(
      @Argument mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): Boolean {
    return localUserService.claimUnverifiedUser(user, mailAddress, password, locale)
  }

  @MutationMapping
  fun updateUserMailAddress(
      @Argument mailAddress: String,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): Boolean {
    localUserService.initiateMailVerification(user, mailAddress, locale)
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
  fun verifyUserMailAddressUnauthenticated(
      @Argument verificationCode: String,
      @Argument userId: UUID,
      @Argument password: String?
  ): Boolean {
    return localUserService.completeMailVerification(userId, verificationCode.toInt(), password)
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

  @MutationMapping
  fun updateUserLanguage(
      @Argument @LanguageIso639 languageCode: String,
      @AuthenticationPrincipal user: User
  ): Boolean {
    user.language = languageCode
    userRepository.save(user)
    return true
  }

  @MutationMapping
  fun deleteUser(@AuthenticationPrincipal user: User): Boolean {
    return userService.markAccountForDeletion(user)
  }

  @MutationMapping
  fun updateUserUiSettings(
      @Argument input: MutableMap<String, Any>,
      @AuthenticationPrincipal user: User
  ): Boolean {
    user.uiSettings.putAll(input)
    userRepository.save(user)
    return true
  }

  @MutationMapping
  fun requestUserPasswordReset(@Argument mailAddress: String, locale: Locale): Boolean {
    val user = userRepository.findByMailAddress(mailAddress) ?: return false
    return localUserService.initiatePasswordReset(user, locale)
  }

  @MutationMapping
  fun resetUserPassword(
      @Argument mailAddress: String,
      @Argument password: String,
      @Argument verificationCode: String
  ): Boolean {
    val user = userRepository.findByMailAddress(mailAddress) ?: return false
    return localUserService.completePasswordReset(user, password, verificationCode.toInt())
  }
}
