/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.user.MAIL_ADDRESS_MAX_LENGTH
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
      @Argument @Email @Size(max = MAIL_ADDRESS_MAX_LENGTH) mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal principal: User,
      locale: Locale
  ): User {
    return localUserService.claimUnverifiedUser(
        currentUser(principal), mailAddress, password, locale)
  }

  @MutationMapping
  fun updateUserMailAddress(
      @Argument @Email @Size(max = MAIL_ADDRESS_MAX_LENGTH) mailAddress: String,
      @Argument password: String,
      @AuthenticationPrincipal principal: User,
      locale: Locale
  ): User {
    return localUserService.initiateMailVerification(
        currentUser(principal), mailAddress, password, locale)
  }

  @MutationMapping
  fun verifyUserMailAddress(
      @Argument @Pattern(regexp = "\\d+") verificationCode: String,
      @AuthenticationPrincipal principal: User
  ): User {
    return localUserService.completeMailVerification(
        currentUser(principal), verificationCode.toInt())
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun verifyUserMailAddressUnauthenticated(
      @Argument @Pattern(regexp = "\\d+") verificationCode: String,
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
      @AuthenticationPrincipal principal: User
  ): User {
    return localUserService.updatePassword(currentUser(principal), oldPassword, newPassword)
  }

  @MutationMapping
  fun updateUserDetails(
      @Argument input: UpdateUserDetailsInput,
      @AuthenticationPrincipal principal: User
  ): User {
    val user = currentUser(principal)
    user.givenName = input.givenName
    user.surname = input.surname
    return userService.save(user)
  }

  @MutationMapping
  fun updateUserLanguage(
      @Argument @LanguageIso639 languageCode: String,
      @AuthenticationPrincipal principal: User
  ): User {
    val user = currentUser(principal)
    user.language = languageCode
    return userService.save(user)
  }

  @MutationMapping
  fun deleteUser(@AuthenticationPrincipal principal: User): UUID {
    return userService.markAccountForDeletion(currentUser(principal)).id!!
  }

  @MutationMapping
  fun updateUserUiSettings(
      @Argument input: MutableMap<String, Any>,
      @AuthenticationPrincipal principal: User
  ): Map<String, Any> {
    val user = currentUser(principal)
    user.uiSettings.putAll(input)
    return userService.save(user).uiSettings
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun requestUserPasswordReset(@Argument mailAddress: String, locale: Locale): Boolean {
    val user =
        userService.findByMailAddress(mailAddress.lowercase()) ?: throw UserNotFoundException()
    localUserService.initiatePasswordReset(user, locale)
    // Returning the user would leak information to a third party
    return true
  }

  @MutationMapping
  @PreAuthorize("hasRole('CHALLENGE_SOLVED')")
  fun resetUserPassword(
      @Argument mailAddress: String,
      @Argument password: String,
      @Argument
      @Pattern(
          regexp = "\\d+",
      )
      verificationCode: String
  ): User {
    val user =
        userService.findByMailAddress(mailAddress.lowercase()) ?: throw UserNotFoundException()
    return localUserService.completePasswordReset(user, password, verificationCode.toInt())
  }

  @MutationMapping
  fun requestUserPasswordSetup(@AuthenticationPrincipal principal: User, locale: Locale): Boolean {
    localUserService.initiatePasswordSetup(currentUser(principal), locale)
    return true
  }

  @MutationMapping
  fun resendVerificationMail(@AuthenticationPrincipal principal: User, locale: Locale): Boolean {
    return localUserService.restartVerification(currentUser(principal), locale)
  }

  /** The account as stored, which is what a request reads and changes, not the principal. */
  private fun currentUser(principal: User): User = userService.getUserById(principal.id!!)
}
