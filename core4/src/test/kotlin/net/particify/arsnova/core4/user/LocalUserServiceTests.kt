/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.exception.InvalidUserStateException
import net.particify.arsnova.core4.user.exception.InvalidVerificationCodeException
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, RecordingMailServiceConfiguration::class)
class LocalUserServiceTests {
  @Autowired lateinit var localUserService: LocalUserServiceImpl
  @Autowired lateinit var mailService: RecordingMailService
  @Autowired lateinit var passwordEncoder: PasswordEncoder

  @Test
  fun shouldClaimUnverifiedUser() {
    val mailAddress = "shouldClaimUnverifiedUser@example.com"
    val user = User()
    Assertions.assertDoesNotThrow {
      localUserService.claimUnverifiedUser(user, mailAddress, "password", Locale.ENGLISH)
    }
    Assertions.assertEquals(mailAddress, user.unverifiedMailAddress)
    Assertions.assertNull(user.mailAddress)
    Assertions.assertNotNull(user.password)
    Assertions.assertNotNull(user.verificationCode)
    Assertions.assertNotNull(user.verificationExpiresAt)
  }

  @Test
  fun shouldNotClaimVerifiedUser() {
    val mailAddress = "shouldNotClaimVerifiedUser@example.com"
    val user = User(mailAddress = "verified-user@example.com", password = "{noop}password")
    Assertions.assertThrows(RuntimeException::class.java) {
      localUserService.claimUnverifiedUser(user, mailAddress, "password", Locale.ENGLISH)
    }
  }

  @Test
  fun shouldInitiateMailVerification() {
    val mailAddress = "shouldInitiateMailVerification@example.com"
    val password = "password"
    val user =
        User(mailAddress = "oldMailAdress@example.com", password = passwordEncoder.encode(password))
    localUserService.initiateMailVerification(user, mailAddress, password, Locale.ENGLISH)
    Assertions.assertEquals(mailAddress, user.unverifiedMailAddress)
    Assertions.assertNotNull(user.verificationCode)
    Assertions.assertNotNull(user.verificationExpiresAt)
  }

  @Test
  fun shouldVerifyMail() {
    val mailAddress = "shouldVerifyMail@example.com"
    val user =
        User(
            unverifiedMailAddress = mailAddress,
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().plusSeconds(10))
    Assertions.assertDoesNotThrow {
      localUserService.completeMailVerification(user, user.verificationCode!!)
    }
  }

  @Test
  fun shouldNotVerifyMailOnCodeMismatch() {
    val mailAddress = "shouldVerifyMail@example.com"
    val user =
        User(
            unverifiedMailAddress = mailAddress,
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().plusSeconds(10))
    Assertions.assertThrows(InvalidVerificationCodeException::class.java) {
      localUserService.completeMailVerification(user, 87654321)
    }
  }

  @Test
  fun shouldNotVerifyMailAfterExpiration() {
    val mailAddress = "shouldVerifyMail@example.com"
    val user =
        User(
            id = UUID.nameUUIDFromBytes("Test User".toByteArray()),
            unverifiedMailAddress = mailAddress,
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().minusSeconds(10))
    Assertions.assertThrows(InvalidUserStateException::class.java) {
      localUserService.completeMailVerification(user, user.verificationCode!!)
    }
  }

  /** A mail address parked by the v3 migration has no verification to restart. */
  @Test
  fun shouldNotRestartVerificationWithoutExpiry() {
    val user =
        User(
            id = UUID.nameUUIDFromBytes("Parked User".toByteArray()),
            unverifiedMailAddress = "parked@example.com")
    Assertions.assertThrows(InvalidUserStateException::class.java) {
      localUserService.restartVerification(user, Locale.ENGLISH)
    }
  }

  @Test
  fun shouldNotInitiateMailVerificationWithoutPassword() {
    val user =
        User(
            id = UUID.nameUUIDFromBytes("Directory User".toByteArray()),
            mailAddress = "shouldNotInitiateMailVerificationWithoutPassword@example.com")
    Assertions.assertThrows(InvalidUserStateException::class.java) {
      localUserService.initiateMailVerification(
          user, "new-address@example.com", "password", Locale.ENGLISH)
    }
  }

  @Test
  fun shouldInitiatePasswordSetup() {
    val user = User(mailAddress = "shouldInitiatePasswordSetup@example.com")
    localUserService.initiatePasswordSetup(user, Locale.ENGLISH)
    Assertions.assertNotNull(user.verificationCode)
    Assertions.assertNotNull(user.verificationExpiresAt)
    Assertions.assertTrue(user.isPasswordResetVerificationActive())
  }

  @Test
  fun shouldSendPasswordSetupMail() {
    val mailAddress = "shouldsendpasswordsetupmail@example.com"
    localUserService.initiatePasswordSetup(User(mailAddress = mailAddress), Locale.ENGLISH)
    Assertions.assertTrue(mailService.recipients.contains(mailAddress))
    Assertions.assertTrue(mailService.templates.contains("password-setup"))
  }

  @Test
  fun shouldNotInitiatePasswordSetupWithPassword() {
    val user =
        User(
            id = UUID.nameUUIDFromBytes("Local Account User".toByteArray()),
            mailAddress = "shouldNotInitiatePasswordSetupWithPassword@example.com",
            password = "{noop}password")
    Assertions.assertThrows(InvalidUserStateException::class.java) {
      localUserService.initiatePasswordSetup(user, Locale.ENGLISH)
    }
  }

  /** Without an address there is nothing to send the code to. */
  @Test
  fun shouldNotInitiatePasswordSetupWithoutMailAddress() {
    val user =
        User(id = UUID.nameUUIDFromBytes("Mailless User".toByteArray()), username = "mailless-user")
    Assertions.assertThrows(InvalidUserStateException::class.java) {
      localUserService.initiatePasswordSetup(user, Locale.ENGLISH)
    }
  }

  /** The setup shares its completion with the reset, which needs no previous password either. */
  @Test
  fun shouldCompletePasswordSetup() {
    val user =
        localUserService.initiatePasswordSetup(
            User(mailAddress = "shouldcompletepasswordsetup@example.com"), Locale.ENGLISH)
    localUserService.completePasswordReset(user, "password", user.verificationCode!!)
    Assertions.assertNotNull(user.password)
    Assertions.assertNotNull(user.passwordChangedAt)
  }
}
