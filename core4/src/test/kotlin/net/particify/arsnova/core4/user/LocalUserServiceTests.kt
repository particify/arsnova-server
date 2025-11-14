/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.util.Locale
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class LocalUserServiceTests {
  @Autowired lateinit var localUserService: LocalUserServiceImpl

  @Test
  fun shouldClaimUnverifiedUser() {
    val mailAddress = "shouldClaimUnverifiedUser@example.com"
    val user = User()
    val result = localUserService.claimUnverifiedUser(user, mailAddress, "password", Locale.ENGLISH)
    Assertions.assertTrue(result)
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
    val result = localUserService.claimUnverifiedUser(user, mailAddress, "password", Locale.ENGLISH)
    Assertions.assertFalse(result)
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
    val result = localUserService.completeMailVerification(user, user.verificationCode!!)
    Assertions.assertTrue(result)
  }

  @Test
  fun shouldNotVerifyMailOnCodeMismatch() {
    val mailAddress = "shouldVerifyMail@example.com"
    val user =
        User(
            unverifiedMailAddress = mailAddress,
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().plusSeconds(10))
    val result = localUserService.completeMailVerification(user, 87654321)
    Assertions.assertFalse(result)
  }

  @Test
  fun shouldNotVerifyMailAfterExpiration() {
    val mailAddress = "shouldVerifyMail@example.com"
    val user =
        User(
            unverifiedMailAddress = mailAddress,
            verificationCode = 12345678,
            verificationExpiresAt = Instant.now().minusSeconds(10))
    val result = localUserService.completeMailVerification(user, user.verificationCode!!)
    Assertions.assertFalse(result)
  }
}
