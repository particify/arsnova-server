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
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["security.login.attempt-limit=$LOGIN_ATTEMPT_LIMIT"])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, LdapTestConfiguration::class)
@WithMockUser(roles = ["CHALLENGE_SOLVED"])
class LdapLoginHttpTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var userService: UserServiceImpl

  private val providerId = UUID.fromString(LDAP_PROVIDER_ID)

  @Test
  fun shouldLoginWithProviderId() {
    val user = HTTP_LOGIN_USER
    login(LDAP_PROVIDER_ID, user.userId, user.password).andExpect(status().isOk())
    val createdUser = userService.loadUserByProviderIdAndExternalId(providerId, user.userId)
    Assertions.assertNotNull(createdUser)
  }

  @Test
  fun shouldRejectLdapCredentialsForLocalProvider() {
    val user = LOCAL_PROVIDER_REJECTION_USER
    login(null, user.userId, user.password).andExpect(status().isUnauthorized())
    val createdUser = userService.loadUserByProviderIdAndExternalId(providerId, user.userId)
    Assertions.assertNull(createdUser)
  }

  @Test
  fun shouldRejectLocalCredentialsForLdapProvider() {
    login(LDAP_PROVIDER_ID, DEV_ACCOUNT_USERNAME, DEV_ACCOUNT_PASSWORD)
        .andExpect(status().isUnauthorized())
    val user = userService.loadUserByProviderIdAndExternalId(providerId, DEV_ACCOUNT_USERNAME)
    Assertions.assertNull(user)
  }

  @Test
  fun shouldRejectUnregisteredProviderId() {
    val user = IMPORT_USER
    login(UUID.randomUUID().toString(), user.userId, user.password)
        .andExpect(status().isBadRequest())
  }

  @Test
  fun shouldRejectMalformedProviderId() {
    val user = IMPORT_USER
    login("not-a-uuid", user.userId, user.password).andExpect(status().isBadRequest())
  }

  @Test
  fun shouldThrottleFailedLoginAttempts() {
    repeat(LOGIN_ATTEMPT_LIMIT) {
      login(LDAP_PROVIDER_ID, THROTTLED_USER_ID, "wrong-password")
          .andExpect(status().isUnauthorized())
    }
    login(LDAP_PROVIDER_ID, THROTTLED_USER_ID, "wrong-password")
        .andExpect(status().isTooManyRequests())
  }

  private fun login(providerIdValue: String?, username: String, password: String): ResultActions {
    val providerIdEntry = providerIdValue?.let { ",\"providerId\":\"$it\"" } ?: ""
    val body = "{\"username\":\"$username\",\"password\":\"$password\"$providerIdEntry}"
    return mockMvc.perform(
        post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
  }
}
