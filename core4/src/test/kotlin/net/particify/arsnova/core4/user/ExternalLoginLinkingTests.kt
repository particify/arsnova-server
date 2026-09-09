/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.security.LdapAuthenticationProviderRegistry
import net.particify.arsnova.core4.user.RecordingExternalLoginLinkingStrategy.Consultation
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

private const val LDAP_LINKING_PROPERTY =
    "security.ldap.registration.$LDAP_PROVIDER_ID.linking-strategy=" +
        RECORDING_LINKING_STRATEGY_NAME
private const val SAML_MAIL_LINKING_PROPERTY =
    "security.saml2.relyingparty.registration.$SAML_MAIL_REGISTRATION_ID.linking-strategy=" +
        RECORDING_LINKING_STRATEGY_NAME
private const val SAML_ID_LINKING_PROPERTY =
    "security.saml2.relyingparty.registration.$SAML_ID_REGISTRATION_ID.linking-strategy=" +
        RECORDING_LINKING_STRATEGY_NAME

/**
 * Covers the [ExternalLoginLinkingStrategy] extension point for both external providers. One
 * context serves both because the hook is identical on either side and a second one would mean a
 * second database. Every registration is configured to select the test strategy here, while the
 * other provider tests boot the same ones without a selection and stay on the unlinked path.
 */
@SpringBootTest(
    properties = [LDAP_LINKING_PROPERTY, SAML_MAIL_LINKING_PROPERTY, SAML_ID_LINKING_PROPERTY])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    ExternalLoginLinkingTestConfiguration::class,
    LdapTestConfiguration::class,
    Saml2TestConfiguration::class)
class ExternalLoginLinkingTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var providerRegistry: LdapAuthenticationProviderRegistry
  @Autowired lateinit var identityProvider: Saml2TestIdentityProvider
  @Autowired lateinit var strategy: RecordingExternalLoginLinkingStrategy
  @Autowired lateinit var transactionManager: PlatformTransactionManager
  @Autowired lateinit var userRepository: UserRepository
  @Autowired lateinit var userService: UserServiceImpl

  private val ldapProviderId = UUID.fromString(LDAP_PROVIDER_ID)
  private val mailRegistrationId = UUID.fromString(SAML_MAIL_REGISTRATION_ID)
  private val idRegistrationId = UUID.fromString(SAML_ID_REGISTRATION_ID)

  @BeforeEach fun resetStrategy() = strategy.reset()

  /**
   * Regression guard for the order of the two steps: the asserted address is dropped as soon as
   * another account holds it, so consulting the strategy afterwards would hand it exactly nothing
   * to match on. The address is passed on as asserted, without being checked against the accounts.
   */
  @Test
  fun shouldConsultStrategyWithAssertedMailAddressHeldByAnotherAccount() {
    val asserted = SAML_LINK_COLLISION_USER
    userRepository.save(User(mailAddress = asserted.mailAddress))

    samlLogin(mailRegistrationId, asserted)

    Assertions.assertEquals(
        listOf(Consultation(mailRegistrationId, asserted.subjectId, asserted.mailAddress)),
        strategy.consultations)
    Assertions.assertNull(loadSamlUser(mailRegistrationId, asserted).mailAddress)
  }

  @Test
  fun shouldAttachSamlLoginToAccountReturnedByStrategy() {
    val asserted = SAML_LINK_TARGET_USER
    val target = userRepository.save(User())
    strategy.armWith(target)
    val accountsBefore = userRepository.count()

    samlLogin(mailRegistrationId, asserted)

    Assertions.assertEquals(accountsBefore, userRepository.count())
    transactional {
      val user = loadSamlUser(mailRegistrationId, asserted)
      Assertions.assertEquals(target.id, user.id)
      Assertions.assertEquals(asserted.mailAddress, user.mailAddress)
      Assertions.assertEquals(mailRegistrationId, user.externalLogins.single().providerId)
    }
  }

  /**
   * The username is the account's public display ID, the string other users type to join a room, so
   * linking must not replace it with the opaque subject ID of the `ID` mapping.
   */
  @Test
  fun shouldKeepUsernameOfLinkedAccountForIdMapping() {
    val asserted = SAML_LINK_ID_USER
    val username = "saml.link.id.display@example.com"
    val target = userRepository.save(User(username = username))
    strategy.armWith(target)

    samlLogin(idRegistrationId, asserted)

    val user = loadSamlUser(idRegistrationId, asserted)
    Assertions.assertEquals(target.id, user.id)
    Assertions.assertEquals(username, user.username)
  }

  @Test
  fun shouldCreateAccountForSamlWithoutLinkTarget() {
    val asserted = SAML_UNLINKED_USER

    samlLogin(mailRegistrationId, asserted)

    Assertions.assertEquals(1, strategy.consultations.size)
    transactional {
      val user = loadSamlUser(mailRegistrationId, asserted)
      Assertions.assertEquals(asserted.mailAddress, user.username)
      Assertions.assertEquals(asserted.mailAddress, user.mailAddress)
      Assertions.assertEquals(asserted.givenName, user.givenName)
      Assertions.assertEquals(asserted.surname, user.surname)
      Assertions.assertTrue(user.roles.any { it.name == "USER" })
      Assertions.assertEquals(mailRegistrationId, user.externalLogins.single().providerId)
    }
  }

  /** The LDAP counterpart of the ordering guard above. */
  @Test
  fun shouldConsultStrategyWithDirectoryMailAddressHeldByAnotherAccount() {
    val directoryUser = LINK_COLLISION_USER
    userRepository.save(User(mailAddress = directoryUser.mailAddress))

    val user = ldapAuthenticate(directoryUser)

    Assertions.assertEquals(
        listOf(Consultation(ldapProviderId, directoryUser.userId, directoryUser.mailAddress)),
        strategy.consultations)
    Assertions.assertNull(user.mailAddress)
  }

  @Test
  fun shouldAttachLdapLoginToAccountReturnedByStrategy() {
    val directoryUser = LINK_TARGET_USER
    val target = userRepository.save(User())
    strategy.armWith(target)
    val accountsBefore = userRepository.count()

    val user = ldapAuthenticate(directoryUser)

    Assertions.assertEquals(accountsBefore, userRepository.count())
    Assertions.assertEquals(target.id, user.id)
    Assertions.assertEquals(directoryUser.mailAddress, user.mailAddress)
    Assertions.assertEquals(target.id, loadLdapUser(directoryUser).id)
  }

  /**
   * `assignUsername` takes the directory ID whenever it is free, so without the guard a linked
   * account would lose the address other users know it by to an internal directory ID.
   */
  @Test
  fun shouldKeepUsernameOfLinkedAccountWhenDirectoryIdIsFree() {
    val directoryUser = LINK_USERNAME_USER
    val username = "ldap.link.display@example.com"
    val target = userRepository.save(User(username = username))
    strategy.armWith(target)
    Assertions.assertFalse(userRepository.existsByUsername(directoryUser.userId))

    val user = ldapAuthenticate(directoryUser)

    Assertions.assertEquals(target.id, user.id)
    Assertions.assertEquals(username, user.username)
  }

  @Test
  fun shouldCreateAccountForLdapWithoutLinkTarget() {
    val directoryUser = UNLINKED_USER

    val user = ldapAuthenticate(directoryUser)

    Assertions.assertEquals(1, strategy.consultations.size)
    Assertions.assertEquals(directoryUser.userId, user.username)
    Assertions.assertEquals(directoryUser.mailAddress, user.mailAddress)
    Assertions.assertEquals(directoryUser.givenName, user.givenName)
    Assertions.assertEquals(directoryUser.surname, user.surname)
    Assertions.assertTrue(user.roles.any { it.name == "USER" })
    Assertions.assertEquals(user.id, loadLdapUser(directoryUser).id)
  }

  private fun samlLogin(registrationId: UUID, user: Saml2TestUser) {
    val encodedResponse = identityProvider.encodedResponse(registrationId, user)
    mockMvc
        .perform(post("/login/saml2/sso/$registrationId").param("SAMLResponse", encodedResponse))
        .andExpect(status().isOk())
  }

  private fun loadSamlUser(registrationId: UUID, user: Saml2TestUser): User =
      checkNotNull(userService.loadUserByProviderIdAndExternalId(registrationId, user.subjectId)) {
        "No account for ${user.subjectId}"
      }

  private fun loadLdapUser(user: LdapTestUser): User =
      checkNotNull(userService.loadUserByProviderIdAndExternalId(ldapProviderId, user.userId)) {
        "No account for ${user.userId}"
      }

  private fun ldapAuthenticate(user: LdapTestUser): User {
    val authenticationManager = checkNotNull(providerRegistry.findByProviderId(ldapProviderId))
    val token = UsernamePasswordAuthenticationToken(user.userId, user.password)
    return authenticationManager.authenticate(token).principal as User
  }

  /** A reloaded account is detached, so its lazy collections need an open transaction. */
  private fun transactional(block: () -> Unit) {
    TransactionTemplate(transactionManager).executeWithoutResult { block() }
  }
}
