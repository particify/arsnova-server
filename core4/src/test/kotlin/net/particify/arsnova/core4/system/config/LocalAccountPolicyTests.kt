/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import net.particify.arsnova.core4.user.ADMIN_ROLE
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val DOMAINS = listOf("example.com", "*.example.org")

class LocalAccountPolicyTests {
  private val guest = User()
  private val administrator =
      User(username = "admin@example.com", roles = mutableSetOf(Role().apply { name = ADMIN_ROLE }))

  @Test
  fun shouldDisableSelfRegistrationWithLocalAccounts() {
    val policy = policy(localAccount(enabled = false, selfRegistrationEnabled = true))
    Assertions.assertFalse(policy.enabled)
    Assertions.assertFalse(policy.selfRegistrationEnabled)
  }

  @Test
  fun shouldAllowAccountCreationForAnyoneWhileSelfRegistrationIsEnabled() {
    val policy = policy(localAccount())
    Assertions.assertTrue(policy.mayCreateAccount(guest))
    Assertions.assertTrue(policy.mayCreateAccount(administrator))
  }

  @Test
  fun shouldRefuseAccountCreationWithoutSelfRegistration() {
    val policy = policy(localAccount(selfRegistrationEnabled = false))
    Assertions.assertFalse(policy.mayCreateAccount(guest))
  }

  /** Administrative account creation is never gated, whichever setting turned the rest off. */
  @Test
  fun shouldAllowAccountCreationForAdministrator() {
    Assertions.assertTrue(
        policy(localAccount(selfRegistrationEnabled = false)).mayCreateAccount(administrator))
    Assertions.assertTrue(policy(localAccount(enabled = false)).mayCreateAccount(administrator))
  }

  /** An invitation puts an address on an account someone else creates, so it is checked too. */
  @Test
  fun shouldRefuseInvitationToDomainOutsideTheRestriction() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.mayInviteMailAddress(guest, "someone@example.com"))
    Assertions.assertFalse(policy.mayInviteMailAddress(guest, "someone@example.net"))
  }

  @Test
  fun shouldAllowInvitationByAdministratorToAnyDomain() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.mayInviteMailAddress(administrator, "someone@example.net"))
  }

  @Test
  fun shouldAllowAnyDomainWithoutRestriction() {
    val policy = policy(localAccount())
    Assertions.assertTrue(policy.isMailAddressAllowed("someone@example.net"))
    Assertions.assertTrue(policy.isMailAddressAllowed("not-an-address"))
  }

  @Test
  fun shouldMatchConfiguredDomainExactly() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.isMailAddressAllowed("someone@example.com"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@example.net"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@other-example.com"))
  }

  /** A wildcard stands for exactly one label, the convention TLS certificates follow. */
  @Test
  fun shouldMatchSingleLabelForWildcard() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.isMailAddressAllowed("someone@mail.example.org"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@example.org"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@mx.mail.example.org"))
  }

  @Test
  fun shouldMatchAnyOfSeveralDomains() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.isMailAddressAllowed("someone@example.com"))
    Assertions.assertTrue(policy.isMailAddressAllowed("someone@mail.example.org"))
  }

  @Test
  fun shouldIgnoreCase() {
    val policy = policy(localAccount(allowedMailAddressDomains = listOf("Example.COM")))
    Assertions.assertTrue(policy.isMailAddressAllowed("Someone@EXAMPLE.com"))
  }

  /** The local part is not the policy's business, so a tagged address passes as v3's did not. */
  @Test
  fun shouldAllowTaggedLocalPart() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.isMailAddressAllowed("someone+tag@example.com"))
    Assertions.assertTrue(policy.isMailAddressAllowed("sömeone@example.com"))
  }

  @Test
  fun shouldTakeDomainAfterLastAtSign() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertTrue(policy.isMailAddressAllowed("\"weird@local\"@example.com"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@example.com@example.net"))
  }

  @Test
  fun shouldRefuseAddressWithoutDomain() {
    val policy = policy(localAccount(allowedMailAddressDomains = DOMAINS))
    Assertions.assertFalse(policy.isMailAddressAllowed("example.com"))
    Assertions.assertFalse(policy.isMailAddressAllowed("someone@"))
  }

  private fun policy(localAccount: SecurityProperties.LocalAccount) =
      LocalAccountPolicy(securityProperties(localAccount = localAccount))
}
