package net.particify.arsnova.gateway.filter

import net.particify.arsnova.gateway.exception.UnauthorizedException
import net.particify.arsnova.gateway.security.JwtTokenUtil
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * This filter extracts the user ID from the JWT, removes the Authorization
 * header and instead adds a header with the user ID.
 *
 * NOTE: This filter is only intended as a temporary solution until we have a
 * better solution to share auth data with services for endpoints that aren't
 * room-specific.
 */
@Component
class JwtUserIdFilter(
  private val jwtTokenUtil: JwtTokenUtil
) : AbstractGatewayFilterFactory<JwtUserIdFilter.Config>(Config::class.java) {
  companion object {
    private const val USER_ID_HEADER = "Arsnova-User-Id"
  }

  private val logger = LoggerFactory.getLogger(AuthFilter::class.java)

  override fun apply(config: Config): GatewayFilter {
    return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
      var request: ServerHttpRequest = exchange.request
      val headers: HttpHeaders = request.headers
      val bearer = headers[HttpHeaders.AUTHORIZATION]

      if (bearer != null) {
        val jwt = bearer[0].removePrefix("Bearer ")
        Mono.just(jwtTokenUtil.getUserIdFromPublicToken(jwt))
          .onErrorResume { exception ->
            logger.debug("Exception on verifying JWT and obtaining userId", exception)
            Mono.error(UnauthorizedException())
          }
          .map { userId: String ->
            logger.trace("Working with userId: {}", userId)
            exchange.mutate().request { r ->
              r.headers { headers ->
                headers.set(USER_ID_HEADER, userId)
                headers.remove(HttpHeaders.AUTHORIZATION)
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
    var name: String = "JwtUseridFilter"
  }
}
