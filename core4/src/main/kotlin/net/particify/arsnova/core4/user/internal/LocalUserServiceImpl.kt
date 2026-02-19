/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.security.SecureRandom
import java.text.MessageFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.system.config.MailProperties
import net.particify.arsnova.core4.user.LocalUserService
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.event.UserMailVerifiedEvent
import net.particify.arsnova.core4.user.event.UserPasswordChangedEvent
import net.particify.arsnova.core4.user.exception.InvalidUserStateException
import net.particify.arsnova.core4.user.exception.InvalidVerificationCodeException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

const val VERIFICATION_VALIDITY_HOURS = 48L
const val VERIFICATION_MAX_ERRORS = 10
const val VERIFICATION_CODE_LENGTH = 6

@Suppress("TooManyFunctions")
@Service
class LocalUserServiceImpl(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService,
    private val eventPublisher: ApplicationEventPublisher,
    mailProperties: MailProperties
) : LocalUserService {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val secureRandom = SecureRandom()
  private val invitationUriPattern = mailProperties.invitationUriPattern
  private val verificationUriPattern = mailProperties.verificationUriPattern
  private val passwordResetUriPattern = mailProperties.passwordResetUriPattern

  fun claimUnverifiedUser(user: User, mailAddress: String, password: String, locale: Locale): User {
    if (user.mailAddress != null) {
      throw InvalidUserStateException("Account not claimable", user.id!!)
    }
    initiateVerification(user)
    user.unverifiedMailAddress = mailAddress
    user.password = passwordEncoder.encode(password)
    val persistedUser = userRepository.save(user)
    sendVerificationMail(persistedUser, "welcome-verification", locale)
    return persistedUser
  }

  fun initiateMailVerification(user: User, mailAddress: String, locale: Locale): User {
    if (user.mailAddress == null) {
      throw InvalidUserStateException("No local login credentials", user.id!!)
    }
    initiateVerification(user)
    user.unverifiedMailAddress = mailAddress
    val persistedUser = userRepository.save(user)
    sendVerificationMail(persistedUser, "mail-verification", locale)
    return persistedUser
  }

  private fun sendVerificationMail(user: User, template: String, locale: Locale) {
    val code = toFixedLength(user.verificationCode!!)
    val verificationUri =
        MessageFormat.format(
            verificationUriPattern,
            user.id,
            code,
            user.verificationExpiresAt?.epochSecond.toString())
    val templateData = mapOf("code" to code, "verificationUri" to verificationUri)
    mailService.sendMail(user.unverifiedMailAddress!!, template, templateData, locale)
  }

  override fun inviteUser(
      inviter: User,
      mailAddress: String,
      template: String,
      data: Map<String, Any>,
      locale: Locale
  ): User {
    val invitee = userService.createAccount()
    initiateVerification(invitee)
    invitee.unverifiedMailAddress = mailAddress
    val persistedInvitee = userRepository.save(invitee)
    val code = toFixedLength(invitee.verificationCode!!)
    val invitationUri =
        MessageFormat.format(
            invitationUriPattern,
            invitee.id,
            code,
            persistedInvitee.verificationExpiresAt?.epochSecond.toString())
    val templateData =
        data.plus(mapOf("code" to code, "inviter" to inviter, "invitationUri" to invitationUri))
    mailService.sendMail(persistedInvitee.unverifiedMailAddress!!, template, templateData, locale)
    return persistedInvitee
  }

  fun completeMailVerification(user: User, verificationCode: Int): User {
    if (!user.isMailAddressVerificationActive()) {
      throw InvalidUserStateException("Mail verification not initiated", user.id!!)
    }
    checkVerificationCode(user, verificationCode)
    user.mailAddress = user.unverifiedMailAddress
    user.username = user.unverifiedMailAddress
    user.resetVerification()
    val persistedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserMailVerifiedEvent(persistedUser.id!!))
    return persistedUser
  }

  fun completeMailVerification(user: User, verificationCode: Int, password: String?): User {
    if (!user.isMailAddressVerificationActive()) {
      throw InvalidUserStateException("Mail verification not initiated", user.id!!)
    }
    checkVerificationCode(user, verificationCode)
    if (!password.isNullOrEmpty()) {
      if (!user.password.isNullOrEmpty()) {
        throw InvalidInputException("Password is already set")
      }
      user.password = passwordEncoder.encode(password)
    }
    return completeMailVerification(user, verificationCode)
  }

  fun initiatePasswordReset(user: User, locale: Locale): User {
    if (user.password == null || user.mailAddress == null) {
      throw InvalidUserStateException("No local login credentials", user.id!!)
    }
    initiateVerification(user)
    val persistedUser = userRepository.save(user)
    sendPasswordResetMail(persistedUser, locale)
    return persistedUser
  }

  private fun sendPasswordResetMail(user: User, locale: Locale) {
    val code = toFixedLength(user.verificationCode!!)
    val passwordResetUri = MessageFormat.format(passwordResetUriPattern, user.mailAddress!!)
    val templateData = mapOf("code" to code, "passwordResetUri" to passwordResetUri)
    mailService.sendMail(user.mailAddress!!, "password-reset", templateData, locale)
  }

  fun completePasswordReset(user: User, password: String, verificationCode: Int): User {
    if (!user.isPasswordResetVerificationActive()) {
      throw InvalidUserStateException("Password reset not initiated", user.id!!)
    }
    checkVerificationCode(user, verificationCode)
    user.password = passwordEncoder.encode(password)
    user.passwordChangedAt = Instant.now()
    user.resetVerification()
    val persistedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserPasswordChangedEvent(persistedUser.id!!))
    return persistedUser
  }

  private fun initiateVerification(user: User) {
    user.resetVerification()
    user.verificationCode = generateNumericCode()
    user.verificationExpiresAt = Instant.now().plus(VERIFICATION_VALIDITY_HOURS, ChronoUnit.HOURS)
    val code = toFixedLength(user.verificationCode!!)
    logger.debug(
        "Generated verification code. User: {}, Code: {}, Expires at: {}",
        user.id,
        code,
        user.verificationExpiresAt)
  }

  private fun checkVerificationCode(user: User, verificationCode: Int) {
    if (user.verificationCode != verificationCode) {
      user.verificationErrors = user.verificationErrors!!.inc()
      userRepository.save(user)
      throw InvalidVerificationCodeException()
    }
  }

  fun updatePassword(user: User, oldPassword: String, newPassword: String): User {
    if (user.password == null) {
      throw InvalidUserStateException("No local login credentials", user.id!!)
    }
    if (!passwordEncoder.matches(oldPassword, user.password)) {
      throw InvalidInputException("Incorrect old password")
    }
    user.password = passwordEncoder.encode(newPassword)
    user.passwordChangedAt = Instant.now()
    val persistedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserPasswordChangedEvent(persistedUser.id!!))
    return persistedUser
  }

  private fun toFixedLength(code: Int): String {
    return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", code)
  }

  private fun generateNumericCode(): Int {
    return secureRandom
        .nextLong(10.0.pow(VERIFICATION_CODE_LENGTH.toDouble()).roundToLong() - 1)
        .toInt()
  }

  fun restartVerification(user: User, locale: Locale): Boolean {
    if (user.unverifiedMailAddress == null) {
      throw InvalidUserStateException("Mail verification not initiated", user.id!!)
    }
    if (Instant.now() > user.verificationExpiresAt) {
      user.verificationErrors = 0
      user.verificationCode = generateNumericCode()
      user.verificationExpiresAt = Instant.now().plus(VERIFICATION_VALIDITY_HOURS, ChronoUnit.HOURS)
      userRepository.save(user)
    }
    sendVerificationMail(user, "welcome-verification", locale)
    return true
  }

  override fun verifyUser(user: User): User {
    if (user.unverifiedMailAddress == null || user.username != null) {
      throw InvalidUserStateException("No verification in process or already verified", user.id!!)
    }
    user.mailAddress = user.unverifiedMailAddress
    user.username = user.unverifiedMailAddress
    user.resetVerification()
    val persistedUser = userRepository.save(user)
    eventPublisher.publishEvent(UserMailVerifiedEvent(persistedUser.id!!))
    return persistedUser
  }
}
