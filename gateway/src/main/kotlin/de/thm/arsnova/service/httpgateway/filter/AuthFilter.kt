package de.thm.arsnova.service.httpgateway.filter

import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.security.JwtTokenUtil
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * This filter is responsible for replacing the JWT from the user with an inter service JWT.
 * It expects a JWT in the Authorization Header, verifies it and extracts the userId.
 * UserId combined with roomId (as a URI variable) are used to query the Auth Service for Room Access.
 */
@Component
class AuthFilter (
        private val jwtTokenUtil: JwtTokenUtil,
        private val roomAccessService: RoomAccessService
) : AbstractGatewayFilterFactory<AuthFilter.Config>(Config::class.java) {

    private val logger = LoggerFactory.getLogger(AuthFilter::class.java)

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            var request: ServerHttpRequest = exchange.request;
            val headers: HttpHeaders = request.headers;
            val bearer = headers.get(HttpHeaders.AUTHORIZATION)

            if (bearer != null) {
                val jwt = bearer[0].removePrefix("Bearer ")
                val userId = jwtTokenUtil.getUserIdFromPublicToken(jwt)
                val uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange)
                val roomId = uriVariables["roomId"]
                if (!roomId!!.matches(RoomIdFilter.roomIdRegex)) {
                    logger.debug("Didn't get a valid roomId out of the uri variables: {}", uriVariables)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST)
                }
                Mono.just(jwtTokenUtil.getUserIdFromPublicToken(jwt))
                    .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST)))
                    .flatMap {
                        roomAccessService.getRoomAccess(roomId, userId)
                    }
                    .onErrorResume {
                        Mono.just(RoomAccess(roomId, userId, "", "PARTICIPANT"))
                    }
                    .map { roomAccess: RoomAccess ->
                        logger.trace("Working with room access: {}", roomAccess)
                        jwtTokenUtil.createSignedInternalToken(roomAccess)
                    }
                    .map { token ->
                        logger.trace("new token: {}", token)
                        exchange.mutate().request { r ->
                            r.headers { headers ->
                                headers.setBearerAuth(token)
                            }
                        }.build()
                    }
                    .defaultIfEmpty(exchange).flatMap(chain::filter)
            } else {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
            }
        }
    }

    class Config {
        var name: String = "AuthFilter"
    }
}
