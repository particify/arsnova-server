/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.Base64
import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.login
import net.particify.arsnova.core4.system.config.securityProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import tools.jackson.databind.json.JsonMapper

/**
 * The `SameSite` and `Partitioned` attributes are only visible in the raw header - neither
 * [MockHttpServletResponse.getCookie] nor the MockMvc cookie matcher exposes them.
 */
class RefreshCookieComponentTests {
  private val servletContext = MockServletContext().apply { contextPath = CONTEXT_PATH }
  private val refreshCookieComponent = componentFor(securityProperties())
  private val jsonMapper = JsonMapper.builder().build()

  @Test
  fun shouldAddCookieWithStrictPolicyByDefault() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(SUBJECT, 1, response)
    val header = setCookieHeader(response, REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(header.contains("; Path=$CONTEXT_PATH"), header)
    Assertions.assertTrue(header.contains("; Secure"), header)
    Assertions.assertTrue(header.contains("; HttpOnly"), header)
    Assertions.assertTrue(header.contains("; SameSite=Strict"), header)
    Assertions.assertFalse(header.contains("; Partitioned"), header)
    val flag = setCookieHeader(response, PARTITIONED_REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(flag.startsWith("$PARTITIONED_REFRESH_TOKEN_COOKIE=;"), flag)
    Assertions.assertTrue(flag.contains("; Max-Age=0"), flag)
  }

  @Test
  fun shouldAddPartitionedCookieWithoutSameSiteRestrictionForCrossSitePolicy() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(SUBJECT, 1, response, RefreshCookiePolicy.CROSS_SITE)
    val header = setCookieHeader(response, REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(header.contains("; Secure"), header)
    Assertions.assertTrue(header.contains("; HttpOnly"), header)
    Assertions.assertTrue(header.contains("; SameSite=None"), header)
    Assertions.assertTrue(header.contains("; Partitioned"), header)
    val flag = setCookieHeader(response, PARTITIONED_REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(flag.startsWith("$PARTITIONED_REFRESH_TOKEN_COOKIE=$FLAG;"), flag)
    Assertions.assertTrue(flag.contains("; Secure"), flag)
    Assertions.assertTrue(flag.contains("; HttpOnly"), flag)
    Assertions.assertTrue(flag.contains("; SameSite=None"), flag)
    Assertions.assertTrue(flag.contains("; Partitioned"), flag)
  }

  /** Max-Age is a duration, so it carries the cookie's lifetime and not its expiration instant. */
  @Test
  fun shouldAddCookieWithSessionLifetimeAsMaxAge() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(SUBJECT, 1, response, RefreshCookiePolicy.CROSS_SITE)
    assertMaxAge(response, SESSION_MAX_AGE)
  }

  @Test
  fun shouldAddCookieWithRememberedLifetimeAsMaxAge() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(
        SUBJECT, 1, response, RefreshCookiePolicy.CROSS_SITE, rememberMe = true)
    assertMaxAge(response, REMEMBERED_MAX_AGE)
  }

  /** Without a configured lifetime the option does not exist, so a client cannot take it. */
  @Test
  fun shouldIgnoreRememberMeWithoutConfiguredLifetime() {
    val response = MockHttpServletResponse()
    val properties = securityProperties(login = login(rememberMeMaxAge = null))
    val component = componentFor(properties)
    component.add(SUBJECT, 1, response, RefreshCookiePolicy.CROSS_SITE, rememberMe = true)
    assertMaxAge(response, SESSION_MAX_AGE)
  }

  /** The lifetime has to survive a rotation, so it is part of the token as well. */
  @Test
  fun shouldEncodeLifetimeAsClaim() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(SUBJECT, 1, response, rememberMe = true)
    val body = tokenBody(setCookieHeader(response, REFRESH_TOKEN_COOKIE))
    Assertions.assertEquals(REMEMBERED_MAX_AGE, (body[EXTEND_BY_CLAIM] as Number).toInt())
  }

  /** The policy is signalled by the flag cookie alone, so it must not reach the token. */
  @Test
  fun shouldEncodeIdenticalTokenBodyForEveryPolicy() {
    val bodies =
        RefreshCookiePolicy.entries.map {
          val response = MockHttpServletResponse()
          refreshCookieComponent.add(SUBJECT, 1, response, it)
          tokenBody(setCookieHeader(response, REFRESH_TOKEN_COOKIE))
        }
    Assertions.assertEquals(bodies.first(), bodies.last())
  }

  /** A cookie is only replaced if the removing one carries the attributes it was added with. */
  @Test
  fun shouldRemoveEveryCookie() {
    val response = MockHttpServletResponse()
    refreshCookieComponent.remove(response)
    val headers = response.getHeaders(HttpHeaders.SET_COOKIE)
    Assertions.assertEquals(RefreshCookiePolicy.entries.size + 1, headers.size)
    Assertions.assertTrue(headers.all { it.contains("; Max-Age=0") }, "$headers")
    Assertions.assertEquals(
        1,
        headers.count { it.isRefreshCookie("Strict") && !it.contains("; Partitioned") },
        "$headers")
    Assertions.assertEquals(
        1, headers.count { it.isRefreshCookie("None") && it.contains("; Partitioned") }, "$headers")
    Assertions.assertEquals(
        1,
        headers.count {
          it.startsWith("$PARTITIONED_REFRESH_TOKEN_COOKIE=;") && it.contains("; Partitioned")
        },
        "$headers")
  }

  private fun assertMaxAge(response: MockHttpServletResponse, maxAge: Int) {
    listOf(REFRESH_TOKEN_COOKIE, PARTITIONED_REFRESH_TOKEN_COOKIE).forEach {
      val header = setCookieHeader(response, it)
      Assertions.assertTrue(header.contains("; Max-Age=$maxAge"), header)
    }
  }

  private fun componentFor(properties: SecurityProperties) =
      RefreshCookieComponent(JwtUtils(properties, null), properties, servletContext)

  private fun String.isRefreshCookie(sameSite: String) =
      startsWith("$REFRESH_TOKEN_COOKIE=;") && contains("; SameSite=$sameSite")

  private fun setCookieHeader(response: MockHttpServletResponse, name: String): String {
    val headers = response.getHeaders(HttpHeaders.SET_COOKIE).filter { it.startsWith("$name=") }
    Assertions.assertEquals(1, headers.size, "$headers")
    return headers.first()
  }

  /** The timestamps are taken per call, so they cannot be part of the comparison. */
  private fun tokenBody(setCookieHeader: String): Map<*, *> {
    val token = setCookieHeader.substringAfter("=").substringBefore(";")
    val body = Base64.getUrlDecoder().decode(token.split(".")[1])
    return jsonMapper.readValue(body, Map::class.java).filterKeys { it !in TIMESTAMP_CLAIMS }
  }

  private companion object {
    const val CONTEXT_PATH = "/api"
    const val EXTEND_BY_CLAIM = "extendBy"
    const val FLAG = "1"

    /** The lifetime configured for a remembered session, 180 days. */
    const val REMEMBERED_MAX_AGE = 15552000

    /** The fixed lifetime of a session which is not remembered, 3 hours. */
    const val SESSION_MAX_AGE = 10800
    const val SUBJECT = "00000000-0000-0000-0000-000000000000"
    val TIMESTAMP_CLAIMS = setOf<Any?>("exp", "iat")
  }
}
