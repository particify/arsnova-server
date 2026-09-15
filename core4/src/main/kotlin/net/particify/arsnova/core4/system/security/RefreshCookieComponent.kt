/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import java.time.Instant
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

/** Name for cookie to store the refresh token. Uses a cookie prefix for additional security. */
const val REFRESH_TOKEN_COOKIE = "__HTTP_ARS_RT"

/** Marks the refresh cookie as partitioned so that a rotation can reuse the policy. */
const val PARTITIONED_REFRESH_TOKEN_COOKIE = "__HTTP_ARS_RT_PARTITIONED"

private const val PARTITIONED_FLAG = "1"
private const val VERSION_CLAIM = "version"

private const val DEFAULT_EXTEND_BY_HOURS = 3L

/** Lifetime of a session which is not remembered. Every rotation slides it forward. */
private val DEFAULT_EXTEND_BY: Duration = Duration.ofHours(DEFAULT_EXTEND_BY_HOURS)

/**
 * Controls whether the refresh cookie is sent by the browser in a cross-site context. The cookie is
 * withheld from any embedded frame unless [CROSS_SITE] is used, so it needs to be chosen per login
 * flow instead of globally. It is signalled by [PARTITIONED_REFRESH_TOKEN_COOKIE], a companion
 * cookie sharing the attributes of [CROSS_SITE], so that it survives the rotation of the refresh
 * cookie - the request refreshing it is same-site and therefore indistinguishable from one of our
 * own UI.
 *
 * That flag is not signed, so anything able to set a cookie on our domain can widen the policy of a
 * victim's refresh cookie. The gain is narrow: a cross-site frame can then trigger refreshes whose
 * response it cannot read because CORS is disabled, while the refresh token stays `HttpOnly`. The
 * flag is `Secure` and `HttpOnly` as well to keep our own origin's JavaScript out of it.
 */
enum class RefreshCookiePolicy(internal val sameSite: String, internal val partitioned: Boolean) {
  /** The cookie is limited to top-level navigation of our own site. */
  STRICT("Strict", false),

  /**
   * The cookie is also sent from a frame embedded by another site, e.g. for an LMS integration.
   * Partitioning (CHIPS) keeps a separate cookie per embedding site, so it cannot be used to track
   * users across sites and is not affected by third-party cookie restrictions.
   */
  CROSS_SITE("None", true)
}

@Component
class RefreshCookieComponent(
    private val jwtUtils: JwtUtils,
    securityProperties: SecurityProperties,
    servletContext: ServletContext
) {
  private val contextPath = servletContext.contextPath
  private val externalSessionMaxAge = securityProperties.login.externalSessionMaxAge
  private val rememberMeMaxAge = securityProperties.login.rememberMeMaxAge

  /** [rememberMe] is without effect unless a remembered lifetime is configured. */
  fun add(
      subject: String,
      version: Int,
      response: HttpServletResponse,
      policy: RefreshCookiePolicy = RefreshCookiePolicy.STRICT,
      rememberMe: Boolean = false
  ) =
      addWithLifetime(
          subject, version, response, policy, RefreshSessionLifetime(extendByFor(rememberMe)))

  /**
   * Starts a session which rests on an external login, so that it ends after
   * `security.login.external-session-max-age` however actively it is used until then. Whichever
   * system owns the account can revoke it without this application ever hearing of it, and
   * repeating the login is the only thing which asks that system again. Without a configured period
   * the session is unbounded like any other.
   */
  fun addForExternalLogin(
      subject: String,
      version: Int,
      response: HttpServletResponse,
      policy: RefreshCookiePolicy = RefreshCookiePolicy.STRICT
  ) =
      addWithLifetime(
          subject,
          version,
          response,
          policy,
          RefreshSessionLifetime(
              DEFAULT_EXTEND_BY, externalSessionMaxAge?.let { Instant.now().plus(it) }))

  /**
   * Removes the cookies for every policy because a browser only replaces a cookie by one carrying
   * the attributes it has been added with, and the policy is unknown outside of a token refresh.
   */
  fun remove(response: HttpServletResponse) {
    RefreshCookiePolicy.entries.forEach { addCookie(response, REFRESH_TOKEN_COOKIE, "", 0, it) }
    addCookie(response, PARTITIONED_REFRESH_TOKEN_COOKIE, "", 0, RefreshCookiePolicy.CROSS_SITE)
  }

  /**
   * Reissues the cookie with the policy the request's own cookies were added with, continuing the
   * session at the lifetime it has been started with.
   */
  fun renew(
      subject: String,
      version: Int,
      lifetime: RefreshSessionLifetime,
      request: HttpServletRequest,
      response: HttpServletResponse
  ) =
      addWithLifetime(
          subject,
          version,
          response,
          policyOf(request),
          RefreshSessionLifetime(extendByFor(lifetime), lifetime.extendUntil))

  private fun addWithLifetime(
      subject: String,
      version: Int,
      response: HttpServletResponse,
      policy: RefreshCookiePolicy,
      lifetime: RefreshSessionLifetime
  ) {
    val maxAge = cappedMaxAge(lifetime)
    val claims = mapOf(VERSION_CLAIM to version).plus(lifetime.toClaims())
    val refreshToken =
        jwtUtils.encodeJwt(subject, listOf(REFRESH_ROLE), claims, Instant.now().plus(maxAge))
    addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, maxAge.seconds, policy)
    addFlagCookie(response, maxAge.seconds, policy)
  }

  /**
   * The deadline shortens the last period of a session so that the token expires with it. A refresh
   * arriving afterwards is rejected before it gets here, see [RefreshSessionLifetime.hasEnded].
   */
  private fun cappedMaxAge(lifetime: RefreshSessionLifetime): Duration {
    val extendBy = extendByFor(lifetime)
    return lifetime.extendUntil?.let { minOf(extendBy, Duration.between(Instant.now(), it)) }
        ?: extendBy
  }

  private fun extendByFor(rememberMe: Boolean) =
      if (rememberMe) rememberMeMaxAge ?: DEFAULT_EXTEND_BY else DEFAULT_EXTEND_BY

  /** A token issued before the lifetime was part of one keeps the lifetime promised back then. */
  private fun extendByFor(lifetime: RefreshSessionLifetime) = lifetime.extendBy ?: extendByFor(true)

  /**
   * The flag is deleted rather than omitted for [RefreshCookiePolicy.STRICT]: a leftover from an
   * earlier cross-site session in the same cookie jar would widen this one at its next refresh.
   */
  private fun addFlagCookie(
      response: HttpServletResponse,
      maxAgeSeconds: Long,
      policy: RefreshCookiePolicy
  ) {
    val crossSite = policy == RefreshCookiePolicy.CROSS_SITE
    addCookie(
        response,
        PARTITIONED_REFRESH_TOKEN_COOKIE,
        if (crossSite) PARTITIONED_FLAG else "",
        if (crossSite) maxAgeSeconds else 0,
        RefreshCookiePolicy.CROSS_SITE)
  }

  private fun policyOf(request: HttpServletRequest) =
      if (request.cookies?.any { it.name == PARTITIONED_REFRESH_TOKEN_COOKIE } == true)
          RefreshCookiePolicy.CROSS_SITE
      else RefreshCookiePolicy.STRICT

  private fun addCookie(
      response: HttpServletResponse,
      name: String,
      value: String,
      maxAgeSeconds: Long,
      policy: RefreshCookiePolicy
  ) {
    // Set-Cookie is built by hand because the servlet API has no support for Partitioned.
    val cookie =
        ResponseCookie.from(name, value)
            .path(contextPath)
            .httpOnly(true)
            .secure(true)
            .maxAge(maxAgeSeconds)
            .partitioned(policy.partitioned)
            .sameSite(policy.sameSite)
            .build()
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
  }
}
