/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.user.exception.InvalidPasswordException
import net.particify.arsnova.core4.user.exception.InvalidUserStateException
import net.particify.arsnova.core4.user.exception.InvalidVerificationCodeException
import net.particify.arsnova.core4.user.exception.WeakPasswordException
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

/** Random, so that nothing but its length and character set make it hard to guess. */
private const val STRONG_PASSWORD = "sXQodP0UYeGQXuXE"

private val WEAK_PASSWORDS = listOf("abcXYZ123!", "AaBbCc123", "1qay2wsx", "P@ssw0rd", "Aaaaaaaa")

/** Strong passwords which cannot be set: two that a login form cannot reproduce, one too long. */
private val INVALID_PASSWORDS =
    listOf(
        "$STRONG_PASSWORD ",
        "\u00A0$STRONG_PASSWORD",
        "sXQodP0U\nYeGQXuXE",
        "ä".repeat(30) + STRONG_PASSWORD)

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
      localUserService.claimUnverifiedUser(user, mailAddress, STRONG_PASSWORD, Locale.ENGLISH)
    }
    Assertions.assertEquals(mailAddress.lowercase(), user.unverifiedMailAddress)
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
    Assertions.assertEquals(mailAddress.lowercase(), user.unverifiedMailAddress)
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
    localUserService.completePasswordReset(user, STRONG_PASSWORD, user.verificationCode!!)
    Assertions.assertNotNull(user.password)
    Assertions.assertNotNull(user.passwordChangedAt)
  }

  @Test
  fun shouldNotClaimUnverifiedUserWithWeakPassword() {
    WEAK_PASSWORDS.forEach {
      val user = User()
      assertThrows<WeakPasswordException>(it) {
        localUserService.claimUnverifiedUser(
            user, "shouldNotClaimWithWeakPassword@example.com", it, Locale.ENGLISH)
      }
      Assertions.assertNull(user.password)
    }
  }

  @Test
  fun shouldNotClaimUnverifiedUserWithInvalidPassword() {
    INVALID_PASSWORDS.forEach {
      val user = User()
      assertThrows<InvalidPasswordException>(it) {
        localUserService.claimUnverifiedUser(
            user, "shouldNotClaimWithInvalidPassword@example.com", it, Locale.ENGLISH)
      }
      Assertions.assertNull(user.password)
      Assertions.assertNull(user.unverifiedMailAddress)
    }
  }

  @Test
  fun shouldClaimUnverifiedUserWithPassphrase() {
    val user = User()
    val passphrase = "quorvex blandimor tessaly"
    localUserService.claimUnverifiedUser(
        user, "shouldClaimWithPassphrase@example.com", passphrase, Locale.ENGLISH)
    Assertions.assertTrue(passwordEncoder.matches(passphrase, user.password))
  }

  @Test
  fun shouldNotVerifyMailWithInvalidPassword() {
    INVALID_PASSWORDS.forEach {
      val user =
          User(
              unverifiedMailAddress = "shouldNotVerifyMailWithInvalidPassword@example.com",
              verificationCode = 12345678,
              verificationExpiresAt = Instant.now().plusSeconds(10))
      assertThrows<InvalidPasswordException>(it) {
        localUserService.completeMailVerification(user, user.verificationCode!!, it)
      }
      Assertions.assertNull(user.password)
    }
  }

  @Test
  fun shouldNotVerifyMailWithWeakPassword() {
    WEAK_PASSWORDS.forEach {
      val user =
          User(
              unverifiedMailAddress = "shouldNotVerifyMailWithWeakPassword@example.com",
              verificationCode = 12345678,
              verificationExpiresAt = Instant.now().plusSeconds(10))
      assertThrows<WeakPasswordException>(it) {
        localUserService.completeMailVerification(user, user.verificationCode!!, it)
      }
      Assertions.assertNull(user.password)
    }
  }

  @Test
  fun shouldVerifyMailWithStrongPassword() {
    val user =
        User(
            unverifiedMailAddress = "shouldVerifyMailWithStrongPassword@example.com",
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().plusSeconds(10))
    localUserService.completeMailVerification(user, user.verificationCode!!, STRONG_PASSWORD)
    Assertions.assertTrue(passwordEncoder.matches(STRONG_PASSWORD, user.password))
  }

  @Test
  fun shouldNotResetToWeakPassword() {
    val user =
        localUserService.initiatePasswordSetup(
            User(mailAddress = "shouldnotresettoweakpassword@example.com"), Locale.ENGLISH)
    WEAK_PASSWORDS.forEach {
      assertThrows<WeakPasswordException>(it) {
        localUserService.completePasswordReset(user, it, user.verificationCode!!)
      }
      Assertions.assertNull(user.password)
    }
  }

  @Test
  fun shouldNotResetToInvalidPassword() {
    val user =
        localUserService.initiatePasswordSetup(
            User(mailAddress = "shouldnotresettoinvalidpassword@example.com"), Locale.ENGLISH)
    INVALID_PASSWORDS.forEach {
      assertThrows<InvalidPasswordException>(it) {
        localUserService.completePasswordReset(user, it, user.verificationCode!!)
      }
      Assertions.assertNull(user.password)
    }
  }

  @Test
  fun shouldNotUpdateToInvalidPassword() {
    val user = localAccountUser("shouldNotUpdateToInvalidPassword@example.com")
    INVALID_PASSWORDS.forEach {
      assertThrows<InvalidPasswordException>(it) {
        localUserService.updatePassword(user, STRONG_PASSWORD, it)
      }
      Assertions.assertTrue(passwordEncoder.matches(STRONG_PASSWORD, user.password))
    }
  }

  /** Passwords set before the rule existed keep working, so the old one is checked unchanged. */
  @Test
  fun shouldUpdateFromPasswordWithOuterWhitespace() {
    val oldPassword = " $STRONG_PASSWORD\t"
    val user =
        User(
            mailAddress = "shouldUpdateFromPasswordWithOuterWhitespace@example.com",
            password = passwordEncoder.encode(oldPassword))
    localUserService.updatePassword(user, oldPassword, STRONG_PASSWORD)
    Assertions.assertTrue(passwordEncoder.matches(STRONG_PASSWORD, user.password))
  }

  @Test
  fun shouldNotUpdateToWeakPassword() {
    val user = localAccountUser("shouldNotUpdateToWeakPassword@example.com")
    WEAK_PASSWORDS.forEach {
      assertThrows<WeakPasswordException>(it) {
        localUserService.updatePassword(user, STRONG_PASSWORD, it)
      }
      Assertions.assertTrue(passwordEncoder.matches(STRONG_PASSWORD, user.password))
    }
  }

  @Test
  fun shouldUpdateToStrongPassword() {
    val user = localAccountUser("shouldUpdateToStrongPassword@example.com")
    val newPassword = STRONG_PASSWORD.reversed()
    localUserService.updatePassword(user, STRONG_PASSWORD, newPassword)
    Assertions.assertTrue(passwordEncoder.matches(newPassword, user.password))
  }

  /** A wrong old password is reported as such, whatever the new one is. */
  @Test
  fun shouldCheckOldPasswordBeforeNewOne() {
    val user = localAccountUser("shouldCheckOldPasswordBeforeNewOne@example.com")
    assertThrows<InvalidInputException> {
      localUserService.updatePassword(user, "wrong-old-password", WEAK_PASSWORDS.first())
    }
  }

  private fun localAccountUser(mailAddress: String) =
      User(mailAddress = mailAddress, password = passwordEncoder.encode(STRONG_PASSWORD))
}
