/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.regex.Pattern
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

private const val GRAPHQL_PATH = "/graphql"
private val BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE)

/**
 * Authenticates a reusable challenge token, on GraphQL requests over HTTP only. Anywhere else the
 * token is not even read, so the request stays unauthenticated.
 *
 * It runs before the filters for the other tokens, which then skip the authenticated request
 * instead of failing to verify this token. Tokens of other kinds are left alone here for the same
 * reason.
 */
@Component
class ReusableChallengeAuthenticationFilter(
    private val tokens: ReusableChallengeTokens,
    servletContext: ServletContext
) : SecurityContextSavingFilter() {
  private val graphQlUri = servletContext.contextPath + GRAPHQL_PATH

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      chain: FilterChain
  ) {
    val header = request.getHeader(HttpHeaders.AUTHORIZATION)
    val matcher = header?.let { BEARER_TOKEN_PATTERN.matcher(it) }
    val token = if (matcher != null && matcher.matches()) matcher.group(1) else null
    if (token != null && tokens.isClaimedBy(token)) {
      try {
        tokens.verify(token)
        saveAuthentication(ReusableChallengeAuthentication(token), request, response)
      } catch (e: JwtException) {
        logger.debug("Reusable challenge token authentication failed", e)
      }
    }
    chain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean =
      request.method != HttpMethod.POST.name() ||
          request.requestURI != graphQlUri ||
          isAuthenticated()
}
