package de.thm.arsnova.service.httpgateway.filter

import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.security.JwtTokenUtil
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

@Component
class RemoveMembershipFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val roomAccessService: RoomAccessService
) : AbstractGatewayFilterFactory<RemoveMembershipFilter.Config>(Config::class.java) {
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            // Both tuple elements are ensured to be there because of previous filters
            Mono.zip(
                Mono.just(ServerWebExchangeUtils.getUriTemplateVariables(exchange))
                    .map { uriVariables ->
                        uriVariables["roomId"]!!
                    },
                Mono.just(exchange.request.headers[HttpHeaders.AUTHORIZATION]!![0])
                    .map { bearer ->
                        bearer.removePrefix("Bearer ")
                    }
                    .map { token ->
                        jwtTokenUtil.getUserIdFromPublicToken(token)
                    }
            )
                .map { t2: Tuple2<String, String> ->
                    // Can be mostly a dummy object as room access service only needs both the IDs
                    RoomAccess(
                        t2.t1,
                        t2.t2,
                        "",
                        "",
                        null
                    )
                }
                .flatMap { roomAccess ->
                    roomAccessService.deleteRoomAccess(roomAccess)
                }
                .map { _ ->
                    exchange.response.statusCode = HttpStatus.OK
                }
                .then()
        }
    }

    class Config {
        var name: String = "CancelMembershipFilter"
    }
}
