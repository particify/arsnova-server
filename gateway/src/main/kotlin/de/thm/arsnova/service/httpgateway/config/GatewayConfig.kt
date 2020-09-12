package de.thm.arsnova.service.httpgateway.config

import de.thm.arsnova.service.httpgateway.filter.AuthFilter
import de.thm.arsnova.service.httpgateway.filter.RequestRateLimiter
import de.thm.arsnova.service.httpgateway.filter.RoomIdFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono


@Configuration
@EnableConfigurationProperties(HttpGatewayProperties::class)
class GatewayConfig (
        private val httpGatewayProperties: HttpGatewayProperties,
        private val requestRateLimiter: RequestRateLimiter
) {
    companion object {
        const val UTIL_PREFIX = "/_util"
    }

    @Bean
    fun ipKeyResolver(): KeyResolver {
        return KeyResolver { exchange -> Mono.just(exchange.request.remoteAddress?.hostName.toString()) }
    }

    @Bean
    fun myRoutes(
            builder: RouteLocatorBuilder,
            authFilter: AuthFilter,
            roomIdFilter: RoomIdFilter
    ): RouteLocator? {
        return builder.routes()
                .route("core") { p ->
                    p
                            .path("/room/**", "/auth/**", "/content/**", "/user/**", "/configuration/**", "/answer/**", "/management/**")
                            .filters { f ->
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.core)
                }
                .route("roomaccess") {p ->
                    p
                            .path("/roomaccess/**")
                            .filters { f ->
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.roomaccessService)
                }
                .route("comment-service") { p ->
                    p
                            .path("/{roomId}/comment/**", "/{roomId}/settings/**")
                            .filters { f ->
                                f.filter(authFilter.apply(AuthFilter.Config()))
                                f.filter(roomIdFilter.apply(RoomIdFilter.Config()))
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.commentService)
                }
                .route("comment-service-todo") { p ->
                    p
                            .path("/bonustoken/**", "/vote/**", "/settings/**")
                            .filters { f ->
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.commentService)
                }
                .route("formatting-service") { p ->
                    p
                            .path("${UTIL_PREFIX}/formatting/render")
                            .filters { f ->
                                f.rewritePath("^${UTIL_PREFIX}/formatting", "")
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.formattingService)
                }
                .build()
    }
}
