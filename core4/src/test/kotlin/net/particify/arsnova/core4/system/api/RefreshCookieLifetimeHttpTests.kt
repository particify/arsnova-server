/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.api

import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.security.REFRESH_TOKEN_COOKIE
import net.particify.arsnova.core4.user.LDAP_PROVIDER_ID
import net.particify.arsnova.core4.user.LdapTestConfiguration
import net.particify.arsnova.core4.user.LdapTestUser
import net.particify.arsnova.core4.user.REMEMBERED_SESSION_USER
import net.particify.arsnova.core4.user.SHORT_SESSION_USER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** The lifetime configured for a remembered session, 180 days. */
private const val REMEMBERED_MAX_AGE = 15552000

/** The fixed lifetime of a session which is not remembered, 3 hours. */
private const val SESSION_MAX_AGE = 10800

/** Which lifetime the login endpoints hand out, as the browser reads it off the cookie. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, LdapTestConfiguration::class)
@WithMockUser(roles = ["CHALLENGE_SOLVED"])
class RefreshCookieLifetimeHttpTests {
  @Autowired lateinit var mockMvc: MockMvc

  @Test
  fun shouldLimitSessionOfPlainLogin() {
    assertMaxAge(login(SHORT_SESSION_USER, rememberMe = false), SESSION_MAX_AGE)
  }

  @Test
  fun shouldRememberSessionOnRequest() {
    assertMaxAge(login(REMEMBERED_SESSION_USER, rememberMe = true), REMEMBERED_MAX_AGE)
  }

  /** A guest account is the cookie, so a limited session would discard the account with it. */
  @Test
  fun shouldRememberSessionOfGuestAccount() {
    val request = post("/auth/guest-account")
    val result = mockMvc.perform(request).andExpect(status().isOk()).andReturn()
    assertMaxAge(result, REMEMBERED_MAX_AGE)
  }

  private fun assertMaxAge(result: MvcResult, maxAge: Int) {
    val headers = result.response.getHeaders(HttpHeaders.SET_COOKIE)
    val header = headers.single { it.startsWith("$REFRESH_TOKEN_COOKIE=") }
    Assertions.assertTrue(header.contains("; Max-Age=$maxAge"), header)
  }

  private fun login(user: LdapTestUser, rememberMe: Boolean): MvcResult {
    val body =
        "{\"username\":\"${user.userId}\",\"password\":\"${user.password}\"," +
            "\"providerId\":\"$LDAP_PROVIDER_ID\",\"rememberMe\":$rememberMe}"
    return mockMvc
        .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andReturn()
  }
}
