/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Drives the SAML converter through Spring Security's filter chain with responses signed in
 * process, which is the only way the update path can be covered: it is reached on a second login,
 * not by calling the converter's helpers.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, Saml2TestConfiguration::class)
class Saml2LoginHttpTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var identityProvider: Saml2TestIdentityProvider
  @Autowired lateinit var transactionManager: PlatformTransactionManager

  private val mailRegistrationId = UUID.fromString(SAML_MAIL_REGISTRATION_ID)
  private val idRegistrationId = UUID.fromString(SAML_ID_REGISTRATION_ID)

  @Test
  fun shouldVerifyNewUserWithMailAddressAsUsername() {
    val asserted = SAML_IMPORT_USER
    login(mailRegistrationId, asserted)
    transactional {
      val user = loadUser(mailRegistrationId, asserted)
      Assertions.assertEquals(asserted.mailAddress, user.username)
      Assertions.assertEquals(asserted.mailAddress, user.mailAddress)
      Assertions.assertEquals(asserted.givenName, user.givenName)
      Assertions.assertEquals(asserted.surname, user.surname)
      Assertions.assertTrue(user.roles.any { it.name == "USER" })
      val externalLogin = user.externalLogins.single()
      Assertions.assertEquals(mailRegistrationId, externalLogin.providerId)
      Assertions.assertNotNull(externalLogin.lastLoginAt)
    }
  }

  @Test
  fun shouldUseLowercasedSubjectIdAsUsernameForIdMapping() {
    val asserted = SAML_MIXED_CASE_ID_USER
    login(idRegistrationId, asserted)
    val user = loadUser(idRegistrationId, asserted)
    Assertions.assertEquals(asserted.subjectId.lowercase(), user.username)
    Assertions.assertEquals(asserted.mailAddress, user.mailAddress)
  }

  /**
   * The asserted address belongs to another account, so it cannot be imported and the mapping has
   * no value left to derive a username from. Neither login may fail over it, and the second one is
   * the update path, where re-asserting the address used to violate the unique constraint. No
   * linking strategy is registered in this context, so this is also what the FOSS build has to keep
   * doing: no account is reused.
   */
  @Test
  fun shouldLeaveUserUnverifiedOnMailAddressCollision() {
    val asserted = SAML_MAIL_COLLISION_USER
    login(mailRegistrationId, asserted)
    val afterFirstLogin = loadUser(mailRegistrationId, asserted)
    Assertions.assertNull(afterFirstLogin.mailAddress)
    Assertions.assertNull(afterFirstLogin.username)

    login(mailRegistrationId, asserted)
    val afterSecondLogin = loadUser(mailRegistrationId, asserted)
    Assertions.assertEquals(afterFirstLogin.id, afterSecondLogin.id)
    Assertions.assertNull(afterSecondLogin.mailAddress)
    Assertions.assertNull(afterSecondLogin.username)
  }

  /**
   * An identity provider which stops releasing the mail attribute must not clear the address the
   * account already holds.
   */
  @Test
  fun shouldKeepStoredMailAddressWhenAssertionCarriesNone() {
    val asserted = SAML_RETAINED_MAIL_USER
    login(mailRegistrationId, asserted)
    Assertions.assertEquals(
        asserted.mailAddress, loadUser(mailRegistrationId, asserted).mailAddress)

    login(mailRegistrationId, asserted.withoutMailAddress())
    val user = loadUser(mailRegistrationId, asserted)
    Assertions.assertEquals(asserted.mailAddress, user.mailAddress)
    Assertions.assertEquals(asserted.mailAddress, user.username)
  }

  /**
   * An identity provider which stops releasing the name attributes must not clear the names the
   * account already holds.
   */
  @Test
  fun shouldKeepStoredNamesWhenAssertionCarriesNone() {
    val asserted = SAML_RETAINED_NAME_USER
    login(mailRegistrationId, asserted)
    val afterFirstLogin = loadUser(mailRegistrationId, asserted)
    Assertions.assertEquals(asserted.givenName, afterFirstLogin.givenName)
    Assertions.assertEquals(asserted.surname, afterFirstLogin.surname)

    login(mailRegistrationId, asserted.copy(givenName = null, surname = null))

    val user = loadUser(mailRegistrationId, asserted)
    Assertions.assertEquals(asserted.givenName, user.givenName)
    Assertions.assertEquals(asserted.surname, user.surname)
  }

  private fun login(registrationId: UUID, user: Saml2TestUser) {
    val encodedResponse = identityProvider.encodedResponse(registrationId, user)
    mockMvc
        .perform(post("/login/saml2/sso/$registrationId").param("SAMLResponse", encodedResponse))
        .andExpect(status().isOk())
  }

  /**
   * A reloaded account is detached, so its lazy collections are only reachable while a transaction
   * is open. The accounts the converter itself creates do not need this, having been populated in
   * memory.
   */
  private fun transactional(block: () -> Unit) {
    TransactionTemplate(transactionManager).executeWithoutResult { block() }
  }

  private fun loadUser(registrationId: UUID, user: Saml2TestUser): User =
      checkNotNull(userService.loadUserByProviderIdAndExternalId(registrationId, user.subjectId)) {
        "No account for ${user.subjectId}"
      }
}
