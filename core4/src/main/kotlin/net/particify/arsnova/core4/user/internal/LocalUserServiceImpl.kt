/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.security.SecureRandom
import java.text.MessageFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToLong
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.system.config.MailProperties
import net.particify.arsnova.core4.user.LocalUserService
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

const val VERIFICATION_VALIDITY_HOURS = 48L
const val VERIFICATION_MAX_ERRORS = 10
const val VERIFICATION_CODE_LENGTH = 6

@Service
class LocalUserServiceImpl(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService,
    mailProperties: MailProperties
) : LocalUserService {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val secureRandom = SecureRandom()
  private val invitationUriPattern = mailProperties.invitationUriPattern
  private val verificationUriPattern = mailProperties.verificationUriPattern
  private val passwordResetUriPattern = mailProperties.passwordResetUriPattern

  fun claimUnverifiedUser(
      user: User,
      mailAddress: String,
      password: String,
      locale: Locale
  ): Boolean {
    if (user.mailAddress != null || user.password != null) {
      return false
    }
    initiateVerification(user)
    user.unverifiedMailAddress = mailAddress
    user.password = passwordEncoder.encode(password)
    userRepository.save(user)
    sendVerificationMail(user, "welcome-verification", locale)
    return true
  }

  fun initiateMailVerification(user: User, mailAddress: String, locale: Locale) {
    initiateVerification(user)
    user.unverifiedMailAddress = mailAddress
    userRepository.save(user)
    sendVerificationMail(user, "mail-verification", locale)
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
    userRepository.save(invitee)
    val code = toFixedLength(invitee.verificationCode!!)
    val invitationUri =
        MessageFormat.format(
            invitationUriPattern,
            invitee.id,
            code,
            invitee.verificationExpiresAt?.epochSecond.toString())
    val templateData =
        data.plus(mapOf("code" to code, "inviter" to inviter, "invitationUri" to invitationUri))
    mailService.sendMail(invitee.unverifiedMailAddress!!, template, templateData, locale)
    return invitee
  }

  fun completeMailVerification(user: User, verificationCode: Int): Boolean {
    if (!user.isMailAddressVerificationActive()) return false
    if (user.verificationCode != verificationCode) {
      user.verificationErrors = user.verificationErrors!!.inc()
      userRepository.save(user)
      return false
    }
    user.mailAddress = user.unverifiedMailAddress
    user.username = user.unverifiedMailAddress
    user.resetVerification()
    userRepository.save(user)
    return true
  }

  fun completeMailVerification(userId: UUID, verificationCode: Int, password: String?): Boolean {
    val user = requireNotNull(userRepository.findByIdOrNull(userId)) { "User not found" }
    check(user.isMailAddressVerificationActive()) { "Verification is not active" }
    if (user.verificationCode != verificationCode) {
      user.verificationErrors = user.verificationErrors!!.inc()
      userRepository.save(user)
      return false
    }
    if (!password.isNullOrEmpty()) {
      if (!user.password.isNullOrEmpty()) {
        error("Password is already set")
      }
      user.password = passwordEncoder.encode(password)
    }
    return completeMailVerification(user, verificationCode)
  }

  fun initiatePasswordReset(user: User, locale: Locale): Boolean {
    if (user.password == null || user.mailAddress == null) {
      return false
    }
    initiateVerification(user)
    userRepository.save(user)
    sendPasswordResetMail(user, locale)
    return true
  }

  private fun sendPasswordResetMail(user: User, locale: Locale) {
    val code = toFixedLength(user.verificationCode!!)
    val passwordResetUri = MessageFormat.format(passwordResetUriPattern, user.mailAddress!!)
    val templateData = mapOf("code" to code, "passwordResetUri" to passwordResetUri)
    mailService.sendMail(user.mailAddress!!, "password-reset", templateData, locale)
  }

  fun completePasswordReset(user: User, password: String, verificationCode: Int): Boolean {
    if (!user.isPasswordResetVerificationActive()) {
      return false
    }
    if (verificationCode != user.verificationCode) {
      user.verificationErrors = user.verificationErrors!!.inc()
      userRepository.save(user)
      return false
    }
    user.password = passwordEncoder.encode(password)
    user.passwordChangedAt = Instant.now()
    user.resetVerification()
    userRepository.save(user)
    return true
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

  fun updatePassword(user: User, oldPassword: String, newPassword: String): Boolean {
    if (user.password != null && !passwordEncoder.matches(oldPassword, user.password)) {
      return false
    }
    user.password = passwordEncoder.encode(newPassword)
    user.passwordChangedAt = Instant.now()
    userRepository.save(user)
    return true
  }

  private fun toFixedLength(code: Int): String {
    return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", code)
  }

  private fun generateNumericCode(): Int {
    return secureRandom
        .nextLong(10.0.pow(VERIFICATION_CODE_LENGTH.toDouble()).roundToLong() - 1)
        .toInt()
  }
}
