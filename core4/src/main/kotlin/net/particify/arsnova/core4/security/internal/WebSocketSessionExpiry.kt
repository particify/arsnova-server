/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import reactor.core.Disposable
import reactor.core.publisher.Mono

/**
 * graphql-ws retries this code with backoff and re-reads its token on reconnect, whereas it gives
 * up on 4401.
 */
private val TOKEN_EXPIRED_STATUS = CloseStatus(4403, "Token expired")

private val logger = LoggerFactory.getLogger(WebSocketSessionExpiry::class.java)

/** Closes a GraphQL WebSocket session once the token that authenticated it has expired. */
@Component
class WebSocketSessionExpiry {
  private val sessions = ConcurrentHashMap<String, TrackedSession>()

  /** Spring for GraphQL hands its interceptors no means to close a session, so track them here. */
  fun decorate(handler: WebSocketHandler): WebSocketHandler =
      object : WebSocketHandlerDecorator(handler) {
        override fun afterConnectionEstablished(session: WebSocketSession) {
          sessions[session.id] = TrackedSession(session)
          super.afterConnectionEstablished(session)
        }

        override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
          sessions.remove(session.id)?.timer?.dispose()
          super.afterConnectionClosed(session, closeStatus)
        }
      }

  fun closeAt(sessionId: String, expiresAt: Instant) {
    val tracked = sessions[sessionId] ?: return
    // A reconnect with the same token would otherwise be accepted and closed again at once.
    val closesAt = expiresAt.plus(JWT_CLOCK_SKEW)
    tracked.closesAt = closesAt
    val delay = Duration.between(Instant.now(), closesAt).coerceAtLeast(Duration.ZERO)
    tracked.timer = Mono.delay(delay).subscribe { close(tracked.session) }
  }

  /** @return whether the session was closed */
  fun closeIfExpired(sessionId: String): Boolean {
    val tracked = sessions[sessionId] ?: return false
    val closesAt = tracked.closesAt ?: return false
    if (Instant.now().isBefore(closesAt)) {
      return false
    }
    close(tracked.session)
    return true
  }

  private fun close(session: WebSocketSession) {
    try {
      session.close(TOKEN_EXPIRED_STATUS)
    } catch (e: IOException) {
      logger.debug("Closing WebSocket session {} failed", session.id, e)
    }
  }

  private class TrackedSession(val session: WebSocketSession) {
    @Volatile var closesAt: Instant? = null
    @Volatile var timer: Disposable? = null
  }
}
