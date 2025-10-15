/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

const val VERIFICATION_VALIDITY_HOURS = 48L
const val VERIFICATION_MAX_ERRORS = 10
const val VERIFICATION_CODE_LENGTH = 6

@Service
class LocalUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val secureRandom = SecureRandom()

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
    val templateData = mapOf("code" to code)
    mailService.sendMail(user.unverifiedMailAddress!!, template, templateData, locale)
  }

  fun completeMailVerification(user: User, verificationCode: Int): Boolean {
    if (!user.isMailAddressVerificationActive()) return false
    if (user.verificationCode != verificationCode) {
      user.verificationErrors!!.inc()
      userRepository.save(user)
      return false
    }
    user.mailAddress = user.unverifiedMailAddress
    user.username = user.unverifiedMailAddress
    user.resetVerification()
    userRepository.save(user)
    return true
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
    val templateData = mapOf("code" to code)
    mailService.sendMail(user.mailAddress!!, "password-reset", templateData, locale)
  }

  fun completePasswordReset(user: User, password: String, verificationCode: Int): Boolean {
    if (!user.isPasswordResetVerificationActive()) {
      return false
    }
    if (verificationCode != user.verificationCode) {
      user.verificationErrors!!.inc()
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
