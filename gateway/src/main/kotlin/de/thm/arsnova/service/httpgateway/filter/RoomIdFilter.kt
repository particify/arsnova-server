package de.thm.arsnova.service.httpgateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange

/**
 * This filter removes the roomId from the HTTP Path to give a cleaner request to services.
 * A route that should use the RoomIdFilter: /<roomId>/comment/<commentId> -> /comment/<commentId>.
 *
 * Throws a Bad Request (400) when no roomId is found.
 */
@Component
class RoomIdFilter : AbstractGatewayFilterFactory<RoomIdFilter.Config>(Config::class.java) {

  companion object {
    val topicRoomIdLength = 32
    val roomIdRegex: Regex = Regex("[0-9a-f]{$topicRoomIdLength}")
    val roomPrefix = "/room"
    val roomIdHeaderName = "ARS_ROOM_ID"
  }

  private val logger = LoggerFactory.getLogger(RoomIdFilter::class.java)

  override fun apply(config: Config): GatewayFilter {
    return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
      val request: ServerHttpRequest = exchange.request
      val uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange)
      val roomId = uriVariables["roomId"]
      if (!roomId!!.matches(roomIdRegex)) {
        logger.debug("Didn't get a valid roomId out of the uri variables: {}", uriVariables)
        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
      }

      // The +1 is for the extra '/' that needs to be cut
      val strippedPath = request.path.value().substring(topicRoomIdLength + 1 + roomPrefix.length)
      var modifiedRequest: ServerHttpRequest = exchange.request
        .mutate().path(strippedPath)
        .header(roomIdHeaderName, roomId)
        .build()

      chain.filter(exchange.mutate().request(modifiedRequest).build())
    }
  }

  class Config {
    var name: String = "RoomIdFilter"
  }
}
