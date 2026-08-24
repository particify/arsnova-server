/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.sdk.ResultCode
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.security.LdapAuthenticationProviderRegistry
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(properties = ["security.login.attempt-limit=$LOGIN_ATTEMPT_LIMIT"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, LdapTestConfiguration::class)
class LdapAuthenticationTests {
  @Autowired lateinit var providerRegistry: LdapAuthenticationProviderRegistry
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var directoryServer: InMemoryDirectoryServer

  private val providerId = UUID.fromString(LDAP_PROVIDER_ID)

  /**
   * Without this, the tests which expect a rejection could pass for the wrong reason: a fixture
   * whose entry failed to load, or whose password does not match, is rejected just the same.
   */
  @Test
  fun shouldProvideFixturesInDirectory() {
    directoryServer.connection.use { connection ->
      for (userId in ABSENT_USER_IDS) {
        Assertions.assertNull(connection.getEntry(userDn(userId)), userId)
      }
      for (user in LDAP_TEST_USERS) {
        val dn = userDn(user.userId)
        val entry = connection.getEntry(dn)
        Assertions.assertNotNull(entry, dn)
        // The user ID attribute is set, so canonicalisation cannot silently fall back to the
        // submitted username.
        Assertions.assertEquals(user.userId, entry.getAttributeValue("uid"), dn)
        val bindResult = connection.bind(dn, user.password)
        Assertions.assertEquals(ResultCode.SUCCESS, bindResult.resultCode, dn)
      }
    }
  }

  @Test
  fun shouldCreateUserWithImportedAttributes() {
    val user = authenticate(IMPORT_USER).principal as User
    Assertions.assertEquals(IMPORT_USER.userId, user.username)
    Assertions.assertEquals(IMPORT_USER.mailAddress, user.mailAddress)
    Assertions.assertEquals(IMPORT_USER.givenName, user.givenName)
    Assertions.assertEquals(IMPORT_USER.surname, user.surname)
    Assertions.assertTrue(user.roles.any { it.name == "USER" })
    val externalLogin = user.externalLogins.single()
    Assertions.assertEquals(providerId, externalLogin.providerId)
    Assertions.assertEquals(IMPORT_USER.userId, externalLogin.externalId)
    Assertions.assertNotNull(externalLogin.lastLoginAt)
  }

  @Test
  fun shouldUseIdFromDirectoryInsteadOfSubmittedUsername() {
    val submittedUsername = CANONICAL_ID_USER.userId.uppercase()
    val user = authenticate(submittedUsername, CANONICAL_ID_USER.password).principal as User
    Assertions.assertEquals(CANONICAL_ID_USER.userId, user.username)
    Assertions.assertEquals(CANONICAL_ID_USER.userId, user.externalLogins.single().externalId)
    Assertions.assertNotNull(
        userService.loadUserByProviderIdAndExternalId(providerId, CANONICAL_ID_USER.userId))
  }

  @Test
  fun shouldReuseUserOnSubsequentLogin() {
    val user = authenticate(REPEAT_LOGIN_USER).principal as User
    val firstLoginAt = user.externalLogins.single().lastLoginAt!!
    val userOfSecondLogin = authenticate(REPEAT_LOGIN_USER).principal as User
    Assertions.assertEquals(user.id, userOfSecondLogin.id)
    Assertions.assertTrue(userOfSecondLogin.externalLogins.single().lastLoginAt!! >= firstLoginAt)
  }

  @Test
  fun shouldNotImportMailAddressAlreadyInUse() {
    val user = authenticate(MAIL_COLLISION_USER).principal as User
    Assertions.assertEquals(MAIL_COLLISION_USER.userId, user.username)
    Assertions.assertNull(user.mailAddress)
  }

  @Test
  fun shouldNotSetUsernameAlreadyInUse() {
    val user = authenticate(USERNAME_COLLISION_USER).principal as User
    Assertions.assertNull(user.username)
    val externalLogin = user.externalLogins.single()
    Assertions.assertEquals(USERNAME_COLLISION_USER.userId, externalLogin.externalId)
  }

  @Test
  fun shouldRejectWrongPassword() {
    val userId = WRONG_PASSWORD_USER.userId
    assertThrows<BadCredentialsException> { authenticate(userId, "not-the-stored-password") }
    Assertions.assertNull(userService.loadUserByProviderIdAndExternalId(providerId, userId))
  }

  @Test
  fun shouldRejectUnknownUser() {
    assertThrows<BadCredentialsException> { authenticate(UNKNOWN_USER_ID, "irrelevant") }
    val user = userService.loadUserByProviderIdAndExternalId(providerId, UNKNOWN_USER_ID)
    Assertions.assertNull(user)
  }

  private fun userDn(userId: String) = "uid=$userId,ou=people,$LDAP_BASE_DN"

  private fun authenticate(user: LdapTestUser): Authentication =
      authenticate(user.userId, user.password)

  private fun authenticate(userId: String, password: String): Authentication {
    val authenticationManager = checkNotNull(providerRegistry.findByProviderId(providerId))
    val token = UsernamePasswordAuthenticationToken(userId, password)
    return authenticationManager.authenticate(token)
  }
}
