/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import jakarta.validation.ConstraintViolationException
import java.time.Duration
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.ExecutionGraphQlResponse
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

private const val PASSWORD = "not-a-real-password"
private const val INVITATION_TEMPLATE = "admin-invitation-verification"
private val TIMEOUT = Duration.ofSeconds(10)

private const val CLAIM_MUTATION =
    "mutation(\$mailAddress: String!) { " +
        "claimUnverifiedUser(mailAddress: \$mailAddress, password: \"$PASSWORD\") { id } }"
private const val UPDATE_MUTATION =
    "mutation(\$mailAddress: String!) { " +
        "updateUserMailAddress(mailAddress: \$mailAddress, password: \"$PASSWORD\") { id } }"
private const val ROOM_INVITATION_MUTATION =
    "mutation(\$mailAddress: String!) { grantRoomRoleByInvitation(" +
        "roomId: \"00000000-0000-0000-0000-000000000000\", mailAddress: \$mailAddress, " +
        "role: MODERATOR) { role } }"
private const val ADMIN_CREATE_MUTATION =
    "mutation(\$mailAddress: String!) { adminCreateUser(mailAddress: \$mailAddress) { id } }"

/**
 * The address is checked by the mutations and again by the service, which other modules and the
 * extensions call directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, RecordingMailServiceConfiguration::class)
class MailAddressValidationTests {
  @Autowired lateinit var localUserService: LocalUserServiceImpl
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var mailService: RecordingMailService
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var graphQlService: ExecutionGraphQlService

  private val administrator =
      User(username = "admin", roles = mutableSetOf(Role().apply { name = ADMIN_ROLE }))

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  @ParameterizedTest
  @MethodSource("validMailAddresses")
  fun shouldAcceptValidMailAddressForClaim(mailAddress: String) {
    val user = localUserService.claimUnverifiedUser(User(), mailAddress, PASSWORD, Locale.ENGLISH)
    Assertions.assertEquals(mailAddress.lowercase(), user.unverifiedMailAddress)
  }

  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldRejectInvalidMailAddressForClaim(mailAddress: String) {
    assertRejectedWithoutMail {
      localUserService.claimUnverifiedUser(User(), mailAddress, PASSWORD, Locale.ENGLISH)
    }
  }

  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldRejectInvalidMailAddressForChange(mailAddress: String) {
    val user = localAccountUser()
    assertRejectedWithoutMail {
      localUserService.initiateMailVerification(user, mailAddress, PASSWORD, Locale.ENGLISH)
    }
    Assertions.assertNull(user.unverifiedMailAddress)
  }

  /** The room module invites through this method, and so do the extensions. */
  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldRejectInvalidMailAddressForInvitation(mailAddress: String) {
    assertRejectedWithoutMail {
      localUserService.inviteUser(
          administrator, mailAddress, INVITATION_TEMPLATE, mapOf(), Locale.ENGLISH)
    }
  }

  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldReportInvalidMailAddressForClaimMutationAsBadRequest(mailAddress: String) {
    val user = userService.createAccount()
    authenticate(user)
    assertBadRequest(execute(CLAIM_MUTATION, mailAddress))
    Assertions.assertNull(userService.findByIdOrNull(user.id!!)!!.unverifiedMailAddress)
  }

  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldReportInvalidMailAddressForChangeMutationAsBadRequest(mailAddress: String) {
    val user = userService.save(localAccountUser())
    authenticate(user)
    assertBadRequest(execute(UPDATE_MUTATION, mailAddress))
    Assertions.assertNull(userService.findByIdOrNull(user.id!!)!!.unverifiedMailAddress)
  }

  /**
   * The room does not exist, so this is answered as a bad request only if the argument is checked
   * before the permission on the room is.
   */
  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldReportInvalidMailAddressForRoomInvitationAsBadRequest(mailAddress: String) {
    authenticate(
        User(username = "verified", roles = mutableSetOf(Role().apply { name = "VERIFIED" })))
    assertBadRequest(execute(ROOM_INVITATION_MUTATION, mailAddress))
  }

  @ParameterizedTest
  @MethodSource("invalidMailAddresses")
  fun shouldReportInvalidMailAddressForAdminCreationAsBadRequest(mailAddress: String) {
    authenticate(administrator)
    assertBadRequest(execute(ADMIN_CREATE_MUTATION, mailAddress))
  }

  private fun assertRejectedWithoutMail(action: () -> Unit) {
    val sentBefore = mailService.recipients.size
    assertThrows<ConstraintViolationException> { action() }
    Assertions.assertEquals(sentBefore, mailService.recipients.size)
  }

  private fun assertBadRequest(response: ExecutionGraphQlResponse) {
    Assertions.assertEquals(
        listOf(ErrorType.BAD_REQUEST), response.errors.map { it.errorType }, response.toString())
  }

  private fun authenticate(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  private fun execute(document: String, mailAddress: String): ExecutionGraphQlResponse {
    val securityContext = SecurityContextHolder.getContext()
    val request =
        DefaultExecutionGraphQlRequest(
            document,
            null,
            mapOf("mailAddress" to mailAddress),
            null,
            UUID.randomUUID().toString(),
            Locale.ENGLISH)
    return graphQlService
        .execute(request)
        .contextWrite { it.put(SecurityContext::class.java.name, securityContext) }
        .block(TIMEOUT)!!
  }

  /** An account with local credentials, which the address change requires. */
  private fun localAccountUser() =
      User(
          mailAddress = "mail-validation-${UUID.randomUUID()}@example.com",
          password = passwordEncoder.encode(PASSWORD),
          roles = mutableSetOf(userService.findRoleByName("USER")))

  private companion object {
    private const val LONG_LABEL_LENGTH = 63

    /** Valid apart from the length: a local part may not exceed 64 characters, a label 63. */
    fun mailAddressOfLength(length: Int): String {
      val prefix = "a@" + "b".repeat(LONG_LABEL_LENGTH) + "."
      val suffix = ".com"
      return prefix + "c".repeat(length - prefix.length - suffix.length) + suffix
    }

    @JvmStatic
    fun validMailAddresses() =
        listOf(
            "a@b.de",
            "a.b+tag@sub.example.co.uk",
            "o'brien@x.de",
            "Mixed.Case@Example.com",
            mailAddressOfLength(MAIL_ADDRESS_MAX_LENGTH))

    @JvmStatic
    fun invalidMailAddresses() =
        listOf(
            "mail@example.com>",
            "'My Name' <name@domain.tld>",
            "<x@example.com> a@example.org",
            "a@example.org, b@example.org",
            " padded@example.org",
            "padded@example.org ",
            mailAddressOfLength(MAIL_ADDRESS_MAX_LENGTH + 1))
  }
}
