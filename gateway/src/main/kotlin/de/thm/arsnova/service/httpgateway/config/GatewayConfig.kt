package de.thm.arsnova.service.httpgateway.config

import de.thm.arsnova.service.httpgateway.filter.AuthFilter
import de.thm.arsnova.service.httpgateway.filter.JwtUserIdFilter
import de.thm.arsnova.service.httpgateway.filter.RequestRateLimiter
import de.thm.arsnova.service.httpgateway.filter.RoomIdFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
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
        return KeyResolver { exchange ->
            Mono.just(
                    listOf(
                            exchange.request.method.toString(),
                            exchange.request.remoteAddress!!.address.toString()
                    )
                            .joinToString(",")
            )
        }
    }

    @Bean
    fun myRoutes(
            builder: RouteLocatorBuilder,
            authFilter: AuthFilter,
            roomIdFilter: RoomIdFilter,
            jwtUserIdFilter: JwtUserIdFilter
    ): RouteLocator? {
        return builder.routes()
                .route("core") { p ->
                    p
                            .path("/room/**", "/auth/**", "/content/**", "/user/**", "/configuration/**", "/answer/**")
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
                .route("import-service") { p ->
                    p
                            .path("/import/**")
                            .filters { f ->
                                f.filter(jwtUserIdFilter.apply(JwtUserIdFilter.Config()))
                                f.requestRateLimiter { r ->
                                    r.rateLimiter = requestRateLimiter
                                }
                            }
                            .uri(httpGatewayProperties.routing.endpoints.importService)
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
                .route("management-core") { p ->
                    p
                            .path(
                                    "/management/core/**"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/core", "/management")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.core)
                }
                .route("management-ws-gateway") { p ->
                    p
                            .path(
                                    "/management/ws-gateway/**"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/ws-gateway", "/management")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.wsGateway)
                }
                .route("management-comment-service") { p ->
                    p
                            .path(
                                    "/management/comment-service/**"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/comment-service", "/management")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.commentService)
                }
                .route("management-auth-service") { p ->
                    p
                            .path(
                                    "/management/auth-service/**"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/auth-service", "/management")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.roomaccessService)
                }
                .route("management-import-service") { p ->
                    p
                            .path(
                                    "/management/import-service/**"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/import-service", "/management")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.importService)
                }
                .route("metrics-proxy") { p ->
                    p
                            .path(
                                    "/management/proxy/prometheus"
                            )
                            .filters { f ->
                                f.rewritePath("^/management/proxy", "")
                            }
                            .uri(httpGatewayProperties.routing.endpoints.proxyMetrics)
                }
                .build()
    }
}
