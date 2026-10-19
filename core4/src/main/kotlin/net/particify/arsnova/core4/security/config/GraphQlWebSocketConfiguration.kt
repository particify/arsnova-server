/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.config

import net.particify.arsnova.core4.security.internal.WebSocketSessionExpiry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.socket.server.support.WebSocketHandlerMapping
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler

/** Spring Boot's mapping for the GraphQL WebSocket endpoint has this order. */
private const val BOOT_GRAPHQL_WEBSOCKET_MAPPING_ORDER = -2

/**
 * Maps the GraphQL WebSocket endpoint ahead of Spring Boot's own mapping, with the same handler and
 * handshake interceptors, so that [WebSocketSessionExpiry] sees every session.
 */
@Configuration
@ConditionalOnProperty("spring.graphql.websocket.path")
class GraphQlWebSocketConfiguration {
  @Bean
  fun expiringGraphQlWebSocketMapping(
      handler: GraphQlWebSocketHandler,
      properties: GraphQlProperties,
      sessionExpiry: WebSocketSessionExpiry
  ): HandlerMapping {
    val requestHandler =
        WebSocketHttpRequestHandler(sessionExpiry.decorate(handler), DefaultHandshakeHandler())
    requestHandler.setHandshakeInterceptors(
        handler.initWebSocketHttpRequestHandler(DefaultHandshakeHandler()).handshakeInterceptors)
    val mapping = WebSocketHandlerMapping()
    mapping.setWebSocketUpgradeMatch(true)
    mapping.urlMap = mapOf(checkNotNull(properties.websocket.path) to requestHandler)
    mapping.order = BOOT_GRAPHQL_WEBSOCKET_MAPPING_ORDER - 1
    return mapping
  }
}
