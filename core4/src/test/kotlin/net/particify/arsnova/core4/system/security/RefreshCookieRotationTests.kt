/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.http.Cookie
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import net.particify.arsnova.core4.system.config.JwtConfiguration
import net.particify.arsnova.core4.system.config.securityProperties
import net.particify.arsnova.core4.user.Role
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

private const val CONTEXT_PATH = "/api"
private const val FLAG = "1"

/**
 * A refresh rotates the cookie, so the policy has to be recovered from the cookies of the request.
 * It is signalled by [PARTITIONED_REFRESH_TOKEN_COOKIE] because the refresh request itself is
 * same-site and carries no indication of the context the cookie is used in.
 */
class RefreshCookieRotationTests {
  private val securityProperties = securityProperties()
  private val jwtUtils = JwtUtils(securityProperties, jwtDecoder(securityProperties.jwt.secret))
  private val user = User(id = UUID.randomUUID())
  private val servletContext = MockServletContext().apply { contextPath = CONTEXT_PATH }
  private val refreshCookieComponent = RefreshCookieComponent(jwtUtils, servletContext)
  private val provider = RefreshJwtAuthenticationProvider(jwtUtils, SingleUserService(user))

  @Test
  fun shouldRenewWithCrossSitePolicyForRequestWithFlag() {
    val headers = renew(issuedCookies(RefreshCookiePolicy.CROSS_SITE))
    val refreshCookie = headerFor(headers, REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(refreshCookie.contains("; SameSite=None"), refreshCookie)
    Assertions.assertTrue(refreshCookie.contains("; Partitioned"), refreshCookie)
    val flag = headerFor(headers, PARTITIONED_REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(flag.startsWith("$PARTITIONED_REFRESH_TOKEN_COOKIE=$FLAG;"), flag)
    Assertions.assertTrue(flag.contains("; Partitioned"), flag)
  }

  @Test
  fun shouldRenewWithStrictPolicyForRequestWithoutFlag() {
    assertRenewedWithStrictPolicy(renew(issuedCookies(RefreshCookiePolicy.STRICT)))
  }

  /** A cookie added before the flag existed keeps the policy it has been added with. */
  @Test
  fun shouldRenewWithStrictPolicyForRequestWithRefreshCookieOnly() {
    assertRenewedWithStrictPolicy(renew(arrayOf(Cookie(REFRESH_TOKEN_COOKIE, "any-token"))))
  }

  /** The rotated cookie has to carry a token which is accepted by the next refresh. */
  @Test
  fun shouldRenewWithUsableRefreshToken() {
    val headers = renew(issuedCookies(RefreshCookiePolicy.CROSS_SITE))
    val token = headerFor(headers, REFRESH_TOKEN_COOKIE).substringAfter("=").substringBefore(";")
    val authentication = provider.authenticate(RefreshJwtAuthentication(token))
    Assertions.assertTrue(authentication.isAuthenticated)
    Assertions.assertEquals(user, authentication.principal)
  }

  private fun assertRenewedWithStrictPolicy(headers: List<String>) {
    val refreshCookie = headerFor(headers, REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(refreshCookie.contains("; SameSite=Strict"), refreshCookie)
    Assertions.assertFalse(refreshCookie.contains("; Partitioned"), refreshCookie)
    val flag = headerFor(headers, PARTITIONED_REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(flag.startsWith("$PARTITIONED_REFRESH_TOKEN_COOKIE=;"), flag)
    Assertions.assertTrue(flag.contains("; Max-Age=0"), flag)
  }

  /** A browser drops a deleted cookie, so it is not sent back with the next request. */
  private fun issuedCookies(policy: RefreshCookiePolicy): Array<Cookie> {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(user.id.toString(), user.tokenVersion!!, response, policy)
    return response.cookies.filter { it.maxAge != 0 }.toTypedArray()
  }

  private fun renew(cookies: Array<Cookie>): List<String> {
    val request = MockHttpServletRequest().apply { setCookies(*cookies) }
    val response = MockHttpServletResponse()
    refreshCookieComponent.renew(user.id.toString(), user.tokenVersion!!, request, response)
    return response.getHeaders(HttpHeaders.SET_COOKIE)
  }

  private fun headerFor(headers: List<String>, name: String): String {
    val matching = headers.filter { it.startsWith("$name=") }
    Assertions.assertEquals(1, matching.size, "$headers")
    return matching.first()
  }

  private fun jwtDecoder(secret: String): JwtDecoder {
    val key = SecretKeySpec(secret.toByteArray(), JwtConfiguration.SECRET_ALGORITHM)
    return NimbusJwtDecoder.withSecretKey(key).build()
  }
}

private class SingleUserService(private val user: User) : UserService {
  override fun loadUserById(id: UUID) = user.takeIf { it.id == id }

  override fun loadUserByUsername(username: String) = error(UNSUPPORTED)

  override fun markAnnouncementsReadForUserId(id: UUID) = error(UNSUPPORTED)

  override fun findRoleByName(name: String): Role = error(UNSUPPORTED)

  override fun createAccount(): User = error(UNSUPPORTED)

  override fun updateLastActivityAt(user: User): User = error(UNSUPPORTED)

  override fun invalidateToken(user: User): User = error(UNSUPPORTED)

  override fun getOrCreateGhostUser(): User = error(UNSUPPORTED)

  private companion object {
    const val UNSUPPORTED = "Not used by the token refresh flow"
  }
}
