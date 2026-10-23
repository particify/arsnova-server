/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.executeAsCurrentUser
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import net.particify.arsnova.core4.user.internal.api.UserMutationController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles

private const val PASSWORD = "not-a-real-password"
private const val STRONG_PASSWORD = "sXQodP0UYeGQXuXE"
private const val VERIFICATION_CODE = 123456

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, RecordingMailServiceConfiguration::class)
@WithMockUser(roles = ["CHALLENGE_SOLVED"])
class UserMutationControllerTests {
  @Autowired lateinit var userMutationController: UserMutationController
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var mailService: RecordingMailService
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var graphQlService: ExecutionGraphQlService

  @Test
  fun shouldRequestPasswordResetForMixedCaseMailAddress() {
    val mailAddress = "Password-Reset-${UUID.randomUUID()}@Example.com"
    userService.save(User(mailAddress = mailAddress, password = passwordEncoder.encode(PASSWORD)))
    Assertions.assertTrue(
        userMutationController.requestUserPasswordReset(mailAddress, Locale.ENGLISH))
    Assertions.assertTrue(mailService.recipients.contains(mailAddress.lowercase()))
  }

  @Test
  fun shouldResetPasswordForMixedCaseMailAddress() {
    val mailAddress = "Password-Reset-${UUID.randomUUID()}@Example.com"
    val newPassword = "another-not-a-real-password"
    userService.save(
        User(
            mailAddress = mailAddress,
            password = passwordEncoder.encode(PASSWORD),
            verificationCode = VERIFICATION_CODE,
            verificationExpiresAt = Instant.now().plus(1, ChronoUnit.HOURS)))
    val user =
        userMutationController.resetUserPassword(
            mailAddress, newPassword, VERIFICATION_CODE.toString())
    Assertions.assertTrue(passwordEncoder.matches(newPassword, user.password))
  }

  /** Clients tell a rejected password apart from other bad requests by the code alone. */
  @Test
  fun shouldReportWeakPasswordWithErrorCode() {
    Assertions.assertEquals(
        mapOf("classification" to "BAD_REQUEST", "code" to "WEAK_PASSWORD"),
        resetPasswordErrorExtensions("P@ssw0rd"))
  }

  @Test
  fun shouldReportInvalidPasswordWithErrorCode() {
    Assertions.assertEquals(
        mapOf("classification" to "BAD_REQUEST", "code" to "INVALID_PASSWORD"),
        resetPasswordErrorExtensions("$STRONG_PASSWORD "))
  }

  /** A password the encoder cannot store is a client error, not a server error. */
  @Test
  fun shouldReportTooLongPasswordAsInvalid() {
    Assertions.assertEquals(
        mapOf("classification" to "BAD_REQUEST", "code" to "INVALID_PASSWORD"),
        resetPasswordErrorExtensions("ä".repeat(30) + STRONG_PASSWORD))
  }

  private fun resetPasswordErrorExtensions(password: String): Any? {
    val mailAddress = "rejected-password-${UUID.randomUUID()}@example.com"
    userService.save(
        User(
            mailAddress = mailAddress,
            password = passwordEncoder.encode(PASSWORD),
            verificationCode = VERIFICATION_CODE,
            verificationExpiresAt = Instant.now().plus(1, ChronoUnit.HOURS)))
    val response =
        graphQlService.executeAsCurrentUser(
            """
            mutation {
              resetUserPassword(
                mailAddress: "$mailAddress", password: "$password", verificationCode: "$VERIFICATION_CODE"
              ) { id }
            }
            """)
    @Suppress("UNCHECKED_CAST")
    val error = (response.toMap()["errors"] as List<Map<String, Any>>).single()
    return error["extensions"]
  }
}
