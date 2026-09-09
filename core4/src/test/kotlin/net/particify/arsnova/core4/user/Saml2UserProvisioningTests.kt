/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import net.particify.arsnova.core4.user.internal.Saml2ResponseAuthenticationConverter
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UsernameMapping
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * Covers the account provisioning of the SAML converter. Driving the conversion itself would mean
 * fabricating a signed response, so the two steps which touch the unique columns are exercised
 * directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class Saml2UserProvisioningTests {
  @Autowired lateinit var converter: Saml2ResponseAuthenticationConverter
  @Autowired lateinit var userRepository: UserRepository

  @Test
  fun shouldAssignMailAddressAsUsername() {
    val user = User(mailAddress = "Saml.Mail.Mapping@example.com")
    converter.assignUsername(user, registration(UsernameMapping.MAIL_ADDRESS), "Subject-1")
    Assertions.assertEquals("saml.mail.mapping@example.com", user.username)
  }

  @Test
  fun shouldAssignLowercasedIdAsUsername() {
    val user = User(mailAddress = "saml.id.mapping@example.com")
    converter.assignUsername(user, registration(UsernameMapping.ID), "Saml-Subject-2")
    Assertions.assertEquals("saml-subject-2", user.username)
  }

  /** An identity provider which does not release the mail attribute must not fail the login. */
  @Test
  fun shouldLeaveUserUnverifiedWithoutMailAddress() {
    val user = User()
    Assertions.assertDoesNotThrow {
      converter.assignUsername(user, registration(UsernameMapping.MAIL_ADDRESS), "Subject-3")
    }
    Assertions.assertNull(user.username)
  }

  @Test
  fun shouldLeaveUserUnverifiedOnUsernameCollision() {
    val taken = "saml-username-collision"
    userRepository.save(User(username = taken))
    val user = User()
    converter.assignUsername(user, registration(UsernameMapping.ID), taken)
    Assertions.assertNull(user.username)
  }

  @Test
  fun shouldImportMailAddress() {
    val user = User()
    converter.updateMailAddress(user, "Saml.Import@example.com")
    Assertions.assertEquals("saml.import@example.com", user.mailAddress)
  }

  /**
   * No linking strategy is registered in this context, so this also pins the behaviour the FOSS
   * build has to keep: an asserted address which is taken is dropped, and no account is reused.
   */
  @Test
  fun shouldNotImportMailAddressAlreadyInUse() {
    val taken = "saml-mail-collision@example.com"
    userRepository.save(User(mailAddress = taken))
    val user = User()
    converter.updateMailAddress(user, taken)
    Assertions.assertNull(user.mailAddress)
  }

  /**
   * The asserted address is written on every login, not just the first, so the update path needs
   * the same guard without giving up an address the account already holds.
   */
  @Test
  fun shouldKeepOwnMailAddressOnRepeatedLogin() {
    val own = "saml-repeated-login@example.com"
    val user = userRepository.save(User(mailAddress = own))
    converter.updateMailAddress(user, own)
    Assertions.assertEquals(own, user.mailAddress)
  }

  /**
   * An identity provider which stops releasing the mail attribute would otherwise clear the address
   * of every account on its next login, turning a change to its configuration into data loss.
   */
  @Test
  fun shouldKeepStoredMailAddressWithoutAssertedAddress() {
    val stored = "saml-missing-attribute@example.com"
    val user = User(mailAddress = stored)
    converter.updateMailAddress(user, null)
    Assertions.assertEquals(stored, user.mailAddress)
  }

  private fun registration(usernameMapping: UsernameMapping): ExtendedRegistration {
    val registration = ExtendedRegistration()
    registration.usernameMapping = usernameMapping
    return registration
  }
}
