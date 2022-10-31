package net.particify.arsnova.gateway.filter

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import net.particify.arsnova.gateway.exception.ForbiddenException
import net.particify.arsnova.gateway.exception.UnauthorizedException
import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.model.RoomFeatures
import net.particify.arsnova.gateway.security.JwtTokenUtil
import net.particify.arsnova.gateway.service.RoomAccessService
import net.particify.arsnova.gateway.service.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3

/**
 * This filter is responsible for replacing the JWT from the user with an inter service JWT.
 * It expects a JWT in the Authorization Header, verifies it and extracts the userId.
 * UserId combined with roomId (as a URI variable) are used to query the Auth Service for Room Access.
 */
@Component
class AuthFilter(
  private val jwtTokenUtil: JwtTokenUtil,
  private val httpGatewayProperties: HttpGatewayProperties,
  private val roomAccessService: RoomAccessService,
  private val subscriptionService: SubscriptionService
) : AbstractGatewayFilterFactory<AuthFilter.Config>(Config::class.java) {

  private val logger = LoggerFactory.getLogger(AuthFilter::class.java)

  override fun apply(config: Config): GatewayFilter {
    return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
      var request: ServerHttpRequest = exchange.request
      val headers: HttpHeaders = request.headers
      val bearer = headers[HttpHeaders.AUTHORIZATION]

      if (bearer != null) {
        val jwt = bearer[0].removePrefix("Bearer ")
        val uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange)
        val roomId = uriVariables["roomId"]
        if (!roomId!!.matches(RoomIdFilter.roomIdRegex)) {
          logger.debug("Didn't get a valid roomId out of the uri variables: {}", uriVariables)
          throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        Mono.just(jwtTokenUtil.getUserIdAndClientRolesFromPublicToken(jwt))
          .onErrorResume { exception ->
            logger.debug("Exception on verifying JWT and obtaining userId", exception)
            Mono.error(UnauthorizedException())
          }
          .flatMap { pair: Pair<String, List<String>> ->
            val userId = pair.first
            val authorities = pair.second
            Mono.zip(
              roomAccessService.getRoomAccess(roomId, userId)
                .onErrorResume(WebClientResponseException::class) { e ->
                  if (e.statusCode != HttpStatus.NOT_FOUND) {
                    logger.error("Unexpected response from auth service")
                    throw e
                  }
                  logger.debug("Auth service did not return a role (user ID: {}, room ID: {})", userId, roomId)
                  if (!config.requireAuthentication) {
                    Mono.just(RoomAccess(roomId, userId, "", "NONE", null))
                  } else if (httpGatewayProperties.gateway.requireMembership && !authorities.contains("ADMIN")) {
                    Mono.error(ForbiddenException())
                  } else {
                    Mono.just(RoomAccess(roomId, userId, "", "PARTICIPANT", null))
                  }
                },
              subscriptionService.getRoomFeatures(roomId, true),
              Mono.just(authorities)
            )
          }
          .map { (roomAccess: RoomAccess, roomFeatures: RoomFeatures, authorityList: List<String>) ->
            logger.trace("Working with roomAccess: {}, roomFeatures: {}, authorityList: {}", roomAccess, roomFeatures, authorityList)
            jwtTokenUtil.createSignedInternalToken(roomAccess, roomFeatures, authorityList)
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

  class Config(val requireAuthentication: Boolean = true) {
    var name: String = "AuthFilter"
  }
}
