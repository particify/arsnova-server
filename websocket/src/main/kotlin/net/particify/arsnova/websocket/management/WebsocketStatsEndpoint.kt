package net.particify.arsnova.websocket.management

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.WebSocketMessageBrokerStats

@Component
@Endpoint(id = "websocket-stats")
class WebsocketStatsEndpoint(private val webSocketMessageBrokerStats: WebSocketMessageBrokerStats) {
  @ReadOperation
  fun readStats(): WebSocketMessageBrokerStats {
    return webSocketMessageBrokerStats
  }
}
