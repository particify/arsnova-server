/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.exception.AccessDeniedException
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.user.exception.AccountCreationNotAllowedException
import net.particify.arsnova.core4.user.exception.MailAddressNotAllowedException
import net.particify.arsnova.core4.user.internal.LocalUserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val ENABLED_PROPERTY = "security.local-account.enabled"
private const val SELF_REGISTRATION_PROPERTY = "security.local-account.self-registration-enabled"
private const val ALLOWED_DOMAINS_PROPERTY = "security.local-account.allowed-mail-address-domains"
private const val ALLOWED_DOMAIN = "example.org"
private const val OTHER_DOMAIN = "example.com"
private const val PASSWORD = "not-a-real-password"
private const val INVITATION_TEMPLATE = "admin-invitation-verification"

class RecordingMailService : MailService {
  val recipients = mutableListOf<String>()

  override fun sendMail(address: String, template: String, data: Map<String, Any>, locale: Locale) {
    recipients.add(address)
  }
}

/** No mail server is reachable from the tests, and no assertion here depends on a sent mail. */
@TestConfiguration(proxyBeanMethods = false)
class RecordingMailServiceConfiguration {
  @Bean @Primary fun recordingMailService() = RecordingMailService()
}

/**
 * The settings are enforced in the service rather than on the mutations, which is what covers the
 * invitation reached through the room module as well.
 */
abstract class LocalAccountEnforcementTestSupport {
  @Autowired lateinit var localUserService: LocalUserServiceImpl
  @Autowired lateinit var mailService: RecordingMailService
  @Autowired lateinit var passwordEncoder: PasswordEncoder

  protected val administrator =
      User(username = "admin", roles = mutableSetOf(Role().apply { name = ADMIN_ROLE }))
  protected val guest = User()

  protected fun claimAccount(mailAddress: String = address(ALLOWED_DOMAIN)): User =
      localUserService.claimUnverifiedUser(User(), mailAddress, PASSWORD, Locale.ENGLISH)

  protected fun inviteUser(inviter: User, mailAddress: String = address(ALLOWED_DOMAIN)): User =
      localUserService.inviteUser(
          inviter, mailAddress, INVITATION_TEMPLATE, mapOf(), Locale.ENGLISH)

  protected fun changeMailAddress(mailAddress: String = address(ALLOWED_DOMAIN)): User =
      localUserService.initiateMailVerification(
          localAccountUser(), mailAddress, PASSWORD, Locale.ENGLISH)

  protected fun requestPasswordReset(): User =
      localUserService.initiatePasswordReset(localAccountUser(), Locale.ENGLISH)

  protected fun completePasswordReset(): User =
      localUserService.completePasswordReset(localAccountUser(), PASSWORD, 0)

  protected fun verifyUser(): User =
      localUserService.verifyUser(User(unverifiedMailAddress = address(ALLOWED_DOMAIN)))

  protected fun address(domain: String) = "local-account-${UUID.randomUUID()}@$domain"

  /** An account with local credentials, which the address change and the reset both require. */
  private fun localAccountUser() =
      User(mailAddress = address(ALLOWED_DOMAIN), password = passwordEncoder.encode(PASSWORD))
}

@SpringBootTest(properties = ["$ENABLED_PROPERTY=false"])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    LdapTestConfiguration::class,
    RecordingMailServiceConfiguration::class)
@WithMockUser(roles = ["CHALLENGE_SOLVED"])
class LocalAccountsDisabledTests : LocalAccountEnforcementTestSupport() {
  @Autowired lateinit var mockMvc: MockMvc

  @Test
  fun shouldRefuseLocalLogin() {
    login(null, DEV_ACCOUNT_USERNAME, DEV_ACCOUNT_PASSWORD).andExpect(status().isBadRequest())
  }

  @Test
  fun shouldStillAllowDirectoryLogin() {
    val user = HTTP_LOGIN_USER
    login(LDAP_PROVIDER_ID, user.userId, user.password).andExpect(status().isOk())
  }

  @Test
  fun shouldRefuseAccountClaim() {
    assertThrows<AccountCreationNotAllowedException> { claimAccount() }
  }

  @Test
  fun shouldRefuseMailAddressChange() {
    assertThrows<AccessDeniedException> { changeMailAddress() }
  }

  @Test
  fun shouldRefusePasswordResetRequest() {
    assertThrows<AccessDeniedException> { requestPasswordReset() }
  }

  @Test
  fun shouldRefusePasswordReset() {
    assertThrows<AccessDeniedException> { completePasswordReset() }
  }

  @Test
  fun shouldStillAllowAdministrativeAccountCreation() {
    Assertions.assertNotNull(inviteUser(administrator))
  }

  @Test
  fun shouldStillAllowAdministrativeVerification() {
    Assertions.assertNotNull(verifyUser().username)
  }

  private fun login(providerId: String?, username: String, password: String): ResultActions {
    val providerIdEntry = providerId?.let { ",\"providerId\":\"$it\"" } ?: ""
    val body = "{\"username\":\"$username\",\"password\":\"$password\"$providerIdEntry}"
    return mockMvc.perform(
        post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
  }
}

@SpringBootTest(properties = ["$SELF_REGISTRATION_PROPERTY=false"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, RecordingMailServiceConfiguration::class)
class SelfRegistrationDisabledTests : LocalAccountEnforcementTestSupport() {
  @Test
  fun shouldRefuseAccountClaim() {
    assertThrows<AccountCreationNotAllowedException> { claimAccount() }
  }

  /** The invitation is the path `grantRoomRoleByInvitation` takes for an unknown address. */
  @Test
  fun shouldRefuseInvitationByOrdinaryUser() {
    assertThrows<AccountCreationNotAllowedException> { inviteUser(guest) }
  }

  @Test
  fun shouldAllowInvitationByAdministrator() {
    val mailAddress = address(ALLOWED_DOMAIN)
    inviteUser(administrator, mailAddress)
    Assertions.assertTrue(mailService.recipients.contains(mailAddress))
  }

  @Test
  fun shouldAllowPasswordResetRequest() {
    Assertions.assertNotNull(requestPasswordReset())
  }

  @Test
  fun shouldAllowMailAddressChange() {
    Assertions.assertNotNull(changeMailAddress())
  }
}

@SpringBootTest(properties = ["$ALLOWED_DOMAINS_PROPERTY[0]=$ALLOWED_DOMAIN"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, RecordingMailServiceConfiguration::class)
class AllowedMailAddressDomainsTests : LocalAccountEnforcementTestSupport() {
  @Test
  fun shouldAllowRegistrationWithinAllowedDomain() {
    Assertions.assertNotNull(claimAccount(address(ALLOWED_DOMAIN)))
  }

  @Test
  fun shouldRefuseRegistrationWithOtherDomain() {
    val exception =
        assertThrows<MailAddressNotAllowedException> { claimAccount(address(OTHER_DOMAIN)) }
    Assertions.assertTrue(exception.message!!.contains(ALLOWED_DOMAIN))
  }

  @Test
  fun shouldAllowMailAddressChangeWithinAllowedDomain() {
    Assertions.assertNotNull(changeMailAddress(address(ALLOWED_DOMAIN)))
  }

  /** Without this check the restriction would only delay a move to any other domain. */
  @Test
  fun shouldRefuseMailAddressChangeToOtherDomain() {
    assertThrows<MailAddressNotAllowedException> { changeMailAddress(address(OTHER_DOMAIN)) }
  }

  @Test
  fun shouldAllowInvitationWithinAllowedDomain() {
    Assertions.assertNotNull(inviteUser(guest, address(ALLOWED_DOMAIN)))
  }

  /** A room's owner invites through the same service, and cannot invite past the restriction. */
  @Test
  fun shouldRefuseInvitationToOtherDomain() {
    assertThrows<MailAddressNotAllowedException> { inviteUser(guest, address(OTHER_DOMAIN)) }
  }

  @Test
  fun shouldAllowAdministrativeInvitationToOtherDomain() {
    Assertions.assertNotNull(inviteUser(administrator, address(OTHER_DOMAIN)))
  }
}
