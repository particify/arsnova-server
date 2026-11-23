/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.AuthenticatedUser
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class UserJwtAuthenticationProviderTests {
  @Autowired lateinit var authenticationProvider: UserJwtAuthenticationProvider
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var userService: UserServiceImpl

  @Test
  fun shouldAuthenticateWithTheAccountsAuthenticationData() {
    val userId = userService.createAccount().id!!
    val authentication = authenticate(userId)
    Assertions.assertEquals(
        AuthenticatedUser(userId, setOf("USER"), false), authentication.principal)
    Assertions.assertTrue(authentication.authorities.any { it.authority == "ROLE_USER" })
    Assertions.assertFalse(authentication.authorities.any { it.authority == "ROLE_VERIFIED" })
  }

  @Test
  fun shouldRejectADeletedAccount() {
    val userId = userService.createAccount().id!!
    authenticate(userId)
    userService.deleteById(userId)
    assertThrows<BadCredentialsException> { authenticate(userId) }
  }

  private fun authenticate(userId: UUID): Authentication =
      authenticationProvider.authenticate(
          UserJwtAuthentication(jwtUtils.encodeJwt(userId.toString(), listOf())))
}
