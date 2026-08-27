/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import net.particify.arsnova.core4.user.internal.ExternalLogin
import net.particify.arsnova.core4.user.internal.UserRepository
import net.particify.arsnova.core4.user.internal.UsernameMapping
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

private val LDAP_PROVIDER_ID = UUID.fromString("b1a0f5c6-6f4a-4a7e-9d2b-1f0c9a7e5d31")
private val SAML_PROVIDER_ID = UUID.fromString("c2b1a6d7-7a5b-4b8f-8e3c-2a1dab8f6e42")

/**
 * The post-passes run against every unsettled account in the database, so these tests assert on the
 * accounts they created and never on totals: other test classes share the schema.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class UserMigrationPostProcessorTests {
  @Autowired lateinit var userRepository: UserRepository
  @Autowired lateinit var migrationProperties: MigrationProperties
  @Autowired lateinit var transactionManager: PlatformTransactionManager
  @PersistenceContext lateinit var entityManager: EntityManager

  @Test
  fun shouldLetLocalAccountKeepSharedMailAddress() {
    val shared = "postpass-local-wins@example.com"
    val local = createLocalUser(shared)
    val external = createExternalUser(LDAP_PROVIDER_ID, "postpass-local-wins-uid", shared)
    settle()
    Assertions.assertEquals(shared, reload(local).mailAddress)
    val settled = reload(external)
    Assertions.assertNull(settled.mailAddress)
    Assertions.assertEquals(shared, settled.unverifiedMailAddress)
    Assertions.assertEquals("postpass-local-wins-uid", settled.username)
  }

  @Test
  fun shouldPromoteParkedMailAddressNobodyHolds() {
    val address = "postpass-free-address@example.com"
    val external = createExternalUser(LDAP_PROVIDER_ID, "postpass-free-uid", address)
    settle()
    val settled = reload(external)
    Assertions.assertEquals(address, settled.mailAddress)
    Assertions.assertNull(settled.unverifiedMailAddress)
    Assertions.assertEquals("postpass-free-uid", settled.username)
  }

  @Test
  fun shouldPromoteSharedMailAddressToMostRecentlyActiveAccount() {
    val shared = "postpass-ranking@example.com"
    val older =
        createExternalUser(
            LDAP_PROVIDER_ID, "postpass-ranking-older", shared, Instant.now().minusSeconds(3600))
    val newer =
        createExternalUser(SAML_PROVIDER_ID, "postpass-ranking-newer", shared, Instant.now())
    settle()
    Assertions.assertEquals(shared, reload(newer).mailAddress)
    val loser = reload(older)
    Assertions.assertNull(loser.mailAddress)
    Assertions.assertEquals(shared, loser.unverifiedMailAddress)
  }

  /** A single statement must not hand the same username to two accounts. */
  @Test
  fun shouldAssignOneUsernameWhenCandidatesCoincide() {
    val first = createExternalUser(LDAP_PROVIDER_ID, "PostPass-Case-Clash", null)
    val second = createExternalUser(SAML_PROVIDER_ID, "postpass-case-clash", null)
    settle()
    val usernames = listOfNotNull(reload(first).username, reload(second).username)
    Assertions.assertEquals(listOf("postpass-case-clash"), usernames)
  }

  @Test
  fun shouldUseMailAddressAsUsernameForMailMappedProvider() {
    val address = "postpass-mail-mapped@example.com"
    val external = createExternalUser(SAML_PROVIDER_ID, "postpass-mail-mapped-subject", address)
    settle(SAML_PROVIDER_ID)
    val settled = reload(external)
    Assertions.assertEquals(address, settled.mailAddress)
    Assertions.assertEquals(address, settled.username)
  }

  @Test
  fun shouldFallBackToExternalIdWhenMailAddressIsNotAvailable() {
    val shared = "postpass-mail-taken@example.com"
    createLocalUser(shared)
    val external = createExternalUser(SAML_PROVIDER_ID, "postpass-mail-taken-subject", shared)
    settle(SAML_PROVIDER_ID)
    val settled = reload(external)
    Assertions.assertNull(settled.mailAddress)
    Assertions.assertEquals("postpass-mail-taken-subject", settled.username)
  }

  @Test
  fun shouldNotChangeAnythingOnRepeatedRun() {
    val shared = "postpass-idempotent@example.com"
    val local = createLocalUser(shared)
    val first = createExternalUser(LDAP_PROVIDER_ID, "postpass-idempotent-a", shared, Instant.now())
    val second =
        createExternalUser(
            SAML_PROVIDER_ID, "postpass-idempotent-b", shared, Instant.now().minusSeconds(60))
    settle()
    val settled = identities(local, first, second)
    settle()
    Assertions.assertEquals(settled, identities(local, first, second))
    Assertions.assertEquals(shared, settled.first().first)
  }

  private fun identities(vararg users: User): List<Pair<String?, String?>> =
      users.map { reload(it) }.map { it.mailAddress to it.username }

  private fun settle(mailMappedProviderId: UUID? = null) {
    val registrations = mutableMapOf<UUID, ExtendedRegistration>()
    val providerMapping = mutableMapOf<String, UUID>()
    if (mailMappedProviderId != null) {
      val registration = ExtendedRegistration()
      registration.usernameMapping = UsernameMapping.MAIL_ADDRESS
      registrations[mailMappedProviderId] = registration
      providerMapping[UserProfile.AuthProvider.SAML.name] = mailMappedProviderId
    }
    val postProcessor =
        UserMigrationPostProcessor(
            entityManager,
            migrationProperties.copy(authenticationProviderMapping = providerMapping),
            ExtendedSaml2RelyingPartyProperties(registrations))
    TransactionTemplate(transactionManager).executeWithoutResult {
      postProcessor.settleIdentities()
    }
  }

  private fun createLocalUser(mailAddress: String): User =
      userRepository.save(User(username = mailAddress, mailAddress = mailAddress))

  private fun createExternalUser(
      providerId: UUID,
      externalId: String,
      parkedMailAddress: String?,
      lastActivityAt: Instant? = null
  ): User {
    val user = User(unverifiedMailAddress = parkedMailAddress, lastActivityAt = lastActivityAt)
    user.externalLogins +=
        ExternalLogin(user = user, providerId = providerId, externalId = externalId)
    return userRepository.save(user)
  }

  private fun reload(user: User): User = userRepository.findByIdOrNull(user.id!!)!!
}
