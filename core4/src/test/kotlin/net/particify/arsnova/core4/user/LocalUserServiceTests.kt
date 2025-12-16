/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.user.exception.InvalidUserStateException
import net.particify.arsnova.core4.user.exception.InvalidVerificationCodeException
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LocalUserServiceTests {
  @Autowired lateinit var localUserService: LocalUserServiceImpl
  @MockitoBean lateinit var mailService: MailService

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
    val user = User()
    localUserService.initiateMailVerification(user, mailAddress, Locale.ENGLISH)
    Assertions.assertEquals(mailAddress, user.unverifiedMailAddress)
    Assertions.assertNull(user.mailAddress)
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
}
