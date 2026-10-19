/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.WebSocketGraphQlRequest
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
    private val userJwtAuthenticationProvider: UserJwtAuthenticationProvider,
    private val sessionExpiry: WebSocketSessionExpiry
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

    return try {
      val authenticatedToken: Authentication =
          userJwtAuthenticationProvider.authenticate(authentication)
      logger.debug("Storing UserJwtAuthentication to session: {}", authenticatedToken)
      SecurityContextHolder.getContext().authentication = authenticatedToken
      (authenticatedToken as? UserJwtAuthentication)?.expiresAt?.let {
        sessionExpiry.closeAt(sessionInfo.id, it)
      }
      Mono.empty()
    } catch (e: AuthenticationException) {
      logger.debug("User JWT authentication failed", e)
      Mono.error(e)
    }
  }

  override fun intercept(
      request: WebGraphQlRequest,
      chain: WebGraphQlInterceptor.Chain
  ): Mono<WebGraphQlResponse> {
    if (request is WebSocketGraphQlRequest &&
        sessionExpiry.closeIfExpired(request.sessionInfo.id)) {
      return Mono.empty()
    }
    return chain.next(request)
  }
}
