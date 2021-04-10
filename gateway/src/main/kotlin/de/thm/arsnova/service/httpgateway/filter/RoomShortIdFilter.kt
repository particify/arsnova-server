package de.thm.arsnova.service.httpgateway.filter

import de.thm.arsnova.service.httpgateway.service.RoomService
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class RoomShortIdFilter(
    private val roomService: RoomService
) : AbstractGatewayFilterFactory<RoomShortIdFilter.Config>(Config::class.java) {

    companion object {
        const val SHORT_ID_LENGTH = 8
        val shortIdRegex = Regex("~[0-9]{$SHORT_ID_LENGTH}")
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange)
            Mono.just(uriVariables)
                .filter { uv -> uv["roomId"] != null }
                .map { uv ->
                    // A bit confusing, but the gateway config does not have separate entries for routes with the shortId
                    uv["roomId"]!!
                }
                .filter { potentialShortId -> potentialShortId.matches(shortIdRegex) }
                .map { shortId ->
                    shortId.drop(1)
                }
                .flatMap { shortId ->
                    roomService.getByShortId(shortId)
                }
                .map { room ->
                    val pathPreChange = exchange.request.path.value()
                    val pathWithRoomId = pathPreChange.replace("~${room.shortId}", room.id)
                    val mutableUriVariables = uriVariables.toMutableMap()
                    mutableUriVariables["roomId"] = room.id
                    ServerWebExchangeUtils.putUriTemplateVariables(exchange, mutableUriVariables)
                    val modifiedRequest = exchange
                        .request
                        .mutate()
                        .path(pathWithRoomId)
                        .build()
                    exchange.mutate().request(modifiedRequest).build()
                }
                .switchIfEmpty(Mono.just(exchange))
                .flatMap { e ->
                    chain.filter(e)
                }
        }
    }

    class Config {
        var name: String = "RoomShortIdFilter"
    }
}