/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import jakarta.servlet.http.Cookie
import java.time.Duration
import java.time.Instant
import java.util.Date
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
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

private const val CONTEXT_PATH = "/api"
private const val FLAG = "1"
private const val ROLES_CLAIM = "roles"
private const val VERSION_CLAIM = "version"

/** The lifetime configured for a remembered session, 180 days. */
private const val REMEMBERED_MAX_AGE = 15552000

/** The fixed lifetime of a session which is not remembered, 3 hours. */
private const val SESSION_MAX_AGE = 10800
private val SESSION_LIFETIME: Duration = Duration.ofSeconds(SESSION_MAX_AGE.toLong())

/** Within the 60 seconds of clock skew the decoder tolerates, so the token still decodes. */
private const val SKEW_SECONDS = 30L

/** Close enough to a deadline for it to shorten the period a rotation would otherwise hand out. */
private const val NEAR_DEADLINE_SECONDS = 600L

private val MAX_AGE_PATTERN = Regex("; Max-Age=(\\d+)")

/** What the provider rejects an ended session with, as opposed to any other refusal. */
private const val ENDED_SESSION_REJECTION = "Session has ended"

/**
 * A refresh rotates the cookie, so the policy has to be recovered from the cookies of the request.
 * It is signalled by [PARTITIONED_REFRESH_TOKEN_COOKIE] because the refresh request itself is
 * same-site and carries no indication of the context the cookie is used in. The lifetime is
 * recovered from the token instead, which the refresh decodes anyway.
 */
class RefreshCookieRotationTests {
  private val securityProperties = securityProperties()
  private val jwtUtils = JwtUtils(securityProperties, jwtDecoder(securityProperties.jwt.secret))
  private val user = User(id = UUID.randomUUID())
  private val servletContext = MockServletContext().apply { contextPath = CONTEXT_PATH }
  private val refreshCookieComponent =
      RefreshCookieComponent(jwtUtils, securityProperties, servletContext)
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

  /** A rotation continues the session, so it cannot decide the lifetime anew. */
  @Test
  fun shouldRenewWithRememberedLifetimeOfToken() {
    assertRenewedWithMaxAge(
        issuedCookies(RefreshCookiePolicy.STRICT, rememberMe = true), REMEMBERED_MAX_AGE)
  }

  @Test
  fun shouldRenewWithSessionLifetimeOfToken() {
    assertRenewedWithMaxAge(issuedCookies(RefreshCookiePolicy.STRICT), SESSION_MAX_AGE)
  }

  /** A token issued before the lifetime was part of one was promised the remembered lifetime. */
  @Test
  fun shouldRenewTokenWithoutLifetimeAsRemembered() {
    val cookies = arrayOf(Cookie(REFRESH_TOKEN_COOKIE, tokenWithoutLifetime()))
    assertRenewedWithMaxAge(cookies, REMEMBERED_MAX_AGE)
  }

  /** A rotation continues a session towards its deadline rather than moving the deadline along. */
  @Test
  fun shouldRenewWithDeadlineOfToken() {
    val cookies = issuedExternalCookies()
    val issued = lifetimeOf(cookies.single { it.name == REFRESH_TOKEN_COOKIE }.value)
    val renewed = lifetimeOf(tokenIn(renew(cookies, issued)))
    Assertions.assertNotNull(issued.extendUntil)
    Assertions.assertEquals(issued.extendUntil, renewed.extendUntil)
  }

  /** The last token of a session reaches exactly as far as the session itself. */
  @Test
  fun shouldRenewWithMaxAgeCappedAtDeadline() {
    val lifetime =
        RefreshSessionLifetime(
            Duration.ofSeconds(SESSION_MAX_AGE.toLong()),
            Instant.now().plusSeconds(NEAR_DEADLINE_SECONDS))
    val headers = renew(issuedCookies(RefreshCookiePolicy.STRICT), lifetime)
    val refreshCookie = headerFor(headers, REFRESH_TOKEN_COOKIE)
    val maxAge = maxAgeOf(refreshCookie)
    Assertions.assertTrue(maxAge in NEAR_DEADLINE_SECONDS - 1..NEAR_DEADLINE_SECONDS, refreshCookie)
  }

  /**
   * The decoder tolerates clock skew, so a token can still be decoded shortly after the session it
   * belongs to has ended. Renewing it would hand out a cookie which deletes itself, so the deadline
   * has to reject the refresh instead.
   */
  @Test
  fun shouldRejectTokenOfSessionEndedWithinClockSkew() {
    val endedAt = Instant.now().minusSeconds(SKEW_SECONDS)
    val token = expiredToken(RefreshSessionLifetime(SESSION_LIFETIME, endedAt))
    val rejection =
        Assertions.assertThrows(BadCredentialsException::class.java) {
          provider.authenticate(RefreshJwtAuthentication(token))
        }
    Assertions.assertEquals(ENDED_SESSION_REJECTION, rejection.message)
  }

  /** The same token without a deadline is accepted, so the deadline is what ends the session. */
  @Test
  fun shouldAcceptSkewedTokenWithoutDeadline() {
    val token = expiredToken(RefreshSessionLifetime(SESSION_LIFETIME))
    Assertions.assertTrue(provider.authenticate(RefreshJwtAuthentication(token)).isAuthenticated)
  }

  /** The lifetime reaches the rotation through the provider, which decodes the token for it. */
  private fun assertRenewedWithMaxAge(cookies: Array<Cookie>, maxAge: Int) {
    val token = cookies.single { it.name == REFRESH_TOKEN_COOKIE }.value
    val authentication = provider.authenticate(RefreshJwtAuthentication(token))
    val lifetime = (authentication as RefreshJwtAuthentication).sessionLifetime
    val refreshCookie = headerFor(renew(cookies, lifetime), REFRESH_TOKEN_COOKIE)
    Assertions.assertTrue(refreshCookie.contains("; Max-Age=$maxAge"), refreshCookie)
  }

  private fun tokenWithoutLifetime() =
      jwtUtils.encodeJwt(
          user.id.toString(), listOf(REFRESH_ROLE), mapOf(VERSION_CLAIM to user.tokenVersion!!))

  /**
   * A token which expired [SKEW_SECONDS] ago and is therefore still within the tolerance of the
   * decoder, issued a whole session before that so it is well-formed. It is signed here rather than
   * by [JwtUtils], which stamps every token as issued now and so cannot produce one whose expiry
   * has already passed: the decoder rejects an expiry preceding the issuing outright, before any
   * tolerance applies.
   */
  private fun expiredToken(lifetime: RefreshSessionLifetime): String {
    val expiresAt = Instant.now().minusSeconds(SKEW_SECONDS)
    val claims =
        JWTClaimsSet.Builder()
            .issuer(securityProperties.jwt.issuer)
            .audience(securityProperties.jwt.issuer)
            .subject(user.id.toString())
            .issueTime(Date.from(expiresAt.minus(SESSION_LIFETIME)))
            .expirationTime(Date.from(expiresAt))
            .claim(ROLES_CLAIM, listOf(REFRESH_ROLE))
            .claim(VERSION_CLAIM, user.tokenVersion!!)
    lifetime.toClaims().forEach { (name, value) -> claims.claim(name, value) }
    val jwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims.build())
    jwt.sign(MACSigner(securityProperties.jwt.secret))
    return jwt.serialize()
  }

  private fun lifetimeOf(token: String) =
      RefreshSessionLifetime.fromClaims(jwtUtils.decodeJwt(token).claims)

  private fun tokenIn(headers: List<String>) =
      headerFor(headers, REFRESH_TOKEN_COOKIE).substringAfter("=").substringBefore(";")

  private fun maxAgeOf(setCookieHeader: String) =
      MAX_AGE_PATTERN.find(setCookieHeader)!!.groupValues[1].toLong()

  private fun issuedExternalCookies(): Array<Cookie> {
    val response = MockHttpServletResponse()
    refreshCookieComponent.addForExternalLogin(
        user.id.toString(), user.tokenVersion!!, response, RefreshCookiePolicy.STRICT)
    return response.cookies.filter { it.maxAge != 0 }.toTypedArray()
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
  private fun issuedCookies(
      policy: RefreshCookiePolicy,
      rememberMe: Boolean = false
  ): Array<Cookie> {
    val response = MockHttpServletResponse()
    refreshCookieComponent.add(
        user.id.toString(), user.tokenVersion!!, response, policy, rememberMe)
    return response.cookies.filter { it.maxAge != 0 }.toTypedArray()
  }

  private fun renew(
      cookies: Array<Cookie>,
      lifetime: RefreshSessionLifetime = RefreshSessionLifetime()
  ): List<String> {
    val request = MockHttpServletRequest().apply { setCookies(*cookies) }
    val response = MockHttpServletResponse()
    refreshCookieComponent.renew(
        user.id.toString(), user.tokenVersion!!, lifetime, request, response)
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
