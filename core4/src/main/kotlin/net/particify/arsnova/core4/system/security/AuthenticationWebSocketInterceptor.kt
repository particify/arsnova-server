/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.WebSocketSessionInfo
import org.springframework.http.HttpHeaders
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthenticationWebSocketInterceptor(
    private val userJwtAuthenticationProvider: UserJwtAuthenticationProvider
) : WebSocketGraphQlInterceptor {
  companion object {
    private val BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE)
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleConnectionInitialization(
      sessionInfo: WebSocketSessionInfo,
      connectionInitPayload: MutableMap<String, Any>
  ): Mono<Any> {
    val jwtHeader = connectionInitPayload[HttpHeaders.AUTHORIZATION] as String

    val tokenMatcher: Matcher = BEARER_TOKEN_PATTERN.matcher(jwtHeader)
    if (!tokenMatcher.matches()) {
      logger.debug("Skipping JWT handling due to pattern mismatch.")
      return Mono.error(AccessDeniedException("JWT pattern mismatch"))
    }
    val authentication = UserJwtAuthentication(tokenMatcher.group(1))

    try {
      val authenticatedToken: Authentication =
          userJwtAuthenticationProvider.authenticate(authentication)
      logger.debug("Storing UserJwtAuthentication to session: $authenticatedToken")
      SecurityContextHolder.getContext().authentication = authenticatedToken
    } catch (e: AuthenticationException) {
      logger.debug("User JWT authentication failed", e)
    }
    return Mono.just(authentication)
  }
}
