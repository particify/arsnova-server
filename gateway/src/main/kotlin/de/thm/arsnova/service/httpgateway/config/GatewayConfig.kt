package de.thm.arsnova.service.httpgateway.config

import de.thm.arsnova.service.httpgateway.filter.AddMembershipFilter
import de.thm.arsnova.service.httpgateway.filter.AddRoomCreatorAccessFilter
import de.thm.arsnova.service.httpgateway.filter.AuthFilter
import de.thm.arsnova.service.httpgateway.filter.JwtUserIdFilter
import de.thm.arsnova.service.httpgateway.filter.RemoveMembershipFilter
import de.thm.arsnova.service.httpgateway.filter.RequestRateLimiter
import de.thm.arsnova.service.httpgateway.filter.RoomIdFilter
import de.thm.arsnova.service.httpgateway.filter.RoomShortIdFilter
import de.thm.arsnova.service.httpgateway.filter.UpdateRoomAccessFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
@EnableConfigurationProperties(HttpGatewayProperties::class)
class GatewayConfig(
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
        addMembershipFilter: AddMembershipFilter,
        removeMembershipFilter: RemoveMembershipFilter,
        roomIdFilter: RoomIdFilter,
        roomShortIdFilter: RoomShortIdFilter,
        roomAuthFilter: UpdateRoomAccessFilter,
        roomCreationAuthFilter: AddRoomCreatorAccessFilter,
        jwtUserIdFilter: JwtUserIdFilter
    ): RouteLocator? {
        val routes = builder.routes()

        routes.route("roomaccess") { p ->
            p
                .path("/roomaccess/**")
                .filters { f ->
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.roomaccessService)
        }

        routes.route("comment-service") { p ->
            p
                .path("/room/{roomId}/comment/**", "/room/{roomId}/settings/**")
                .filters { f ->
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.commentService)
        }

        routes.route("comment-service-todo") { p ->
            p
                .path(
                    "/room/{roomId}/bonustoken/**",
                    "/room/{roomId}/vote/**",
                    "/room/{roomId}/settings/**"
                )
                .filters { f ->
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.commentService)
        }

        if (httpGatewayProperties.routing.endpoints.importService != null) {
            routes.route("import-service") { p ->
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
        }

        routes.route("formatting-service") { p ->
            p
                .path("$UTIL_PREFIX/formatting/render*")
                .filters { f ->
                    f.rewritePath("^$UTIL_PREFIX/formatting", "")
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.formattingService)
        }

        if (httpGatewayProperties.routing.endpoints.attachmentService != null) {
            routes.route("attachment-service") { p ->
                p
                    .path(
                        "/room/{roomId}/filemetadata/**",
                        "/room/{roomId}/file/**"
                    )
                    .filters { f ->
                        f.filter(authFilter.apply(AuthFilter.Config()))
                        f.filter(roomIdFilter.apply(RoomIdFilter.Config()))
                        f.requestRateLimiter { r ->
                            r.rateLimiter = requestRateLimiter
                        }
                    }
                    .uri(httpGatewayProperties.routing.endpoints.attachmentService)
            }
            routes.route("attachment-service") { p ->
                p
                    .path(
                        "/quota/**"
                    )
                    .filters { f ->
                        f.requestRateLimiter { r ->
                            r.rateLimiter = requestRateLimiter
                        }
                    }
                    .uri(httpGatewayProperties.routing.endpoints.attachmentService)
            }
        }

        if (httpGatewayProperties.routing.endpoints.subscriptionService != null) {
            routes.route("subscription-service") { p ->
                p
                    .path(
                        "/feature/**",
                        "/featuresettings/**",
                        "/subscription/**",
                        "/tier/**"
                    )
                    .filters { f ->
                        f.requestRateLimiter { r ->
                            r.rateLimiter = requestRateLimiter
                        }
                    }
                    .uri(httpGatewayProperties.routing.endpoints.subscriptionService)
            }
        }

        routes.route("management-core") { p ->
            p
                .path(
                    "/management/core/**"
                )
                .filters { f ->
                    f.rewritePath("^/management/core", "/management")
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("management-ws-gateway") { p ->
            p
                .path(
                    "/management/ws-gateway/**"
                )
                .filters { f ->
                    f.rewritePath("^/management/ws-gateway", "/management")
                }
                .uri(httpGatewayProperties.routing.endpoints.wsGateway)
        }

        routes.route("management-comment-service") { p ->
            p
                .path(
                    "/management/comment-service/**"
                )
                .filters { f ->
                    f.rewritePath("^/management/comment-service", "/management")
                }
                .uri(httpGatewayProperties.routing.endpoints.commentService)
        }

        routes.route("management-auth-service") { p ->
            p
                .path(
                    "/management/auth-service/**"
                )
                .filters { f ->
                    f.rewritePath("^/management/auth-service", "/management")
                }
                .uri(httpGatewayProperties.routing.endpoints.roomaccessService)
        }

        if (httpGatewayProperties.routing.endpoints.importService != null) {
            routes.route("management-import-service") { p ->
                p
                    .path(
                        "/management/import-service/**"
                    )
                    .filters { f ->
                        f.rewritePath("^/management/import-service", "/management")
                    }
                    .uri(httpGatewayProperties.routing.endpoints.importService)
            }
        }

        if (httpGatewayProperties.routing.endpoints.proxyMetrics != null) {
            routes.route("metrics-proxy") { p ->
                p
                    .path(
                        "/management/proxy/prometheus"
                    )
                    .filters { f ->
                        f.rewritePath("^/management/proxy", "")
                    }
                    .uri(httpGatewayProperties.routing.endpoints.proxyMetrics)
            }
        }

        routes.route("request-membership") { p ->
            p
                .path(
                    "/room/{roomId}/request-membership"
                )
                .filters { f ->
                    f.filter(addMembershipFilter.apply(AddMembershipFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("cancel-membership") { p ->
            p
                .path(
                    "/room/{roomId}/cancel-membership"
                )
                .filters { f ->
                    f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.filter(removeMembershipFilter.apply(RemoveMembershipFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("core") { p ->
            p
                .path(
                    "/room/{roomId}/content/**",
                    "/room/{roomId}/contentgroup/**",
                    "/room/{roomId}/answer/**"
                )
                .filters { f ->
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.filter(roomIdFilter.apply(RoomIdFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("room-duplicate") { p ->
            p
                .path(
                    "/room/{roomId}/duplicate"
                )
                .filters { f ->
                    f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
                    f.filter(authFilter.apply(AuthFilter.Config(false)))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("room-post") { p ->
            p
                .path(
                    "/room/"
                )
                .filters { f ->
                    f.filter(roomCreationAuthFilter.apply(AddRoomCreatorAccessFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("room") { p ->
            p
                .path(
                    "/room/{roomId}/moderator/**",
                    "/room/{roomId}/transfer**"
                )
                .filters { f ->
                    f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.filter(roomAuthFilter.apply(UpdateRoomAccessFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("core") { p ->
            p
                .path(
                    "/room/{roomId}/**"
                )
                .filters { f ->
                    f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
                    f.filter(authFilter.apply(AuthFilter.Config()))
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        routes.route("core-without-room-auth") { p ->
            p
                .path(
                    "/auth/**",
                    "/user/**",
                    "/configuration/**"
                )
                .filters { f ->
                    f.requestRateLimiter { r ->
                        r.rateLimiter = requestRateLimiter
                    }
                }
                .uri(httpGatewayProperties.routing.endpoints.core)
        }

        return routes.build()
    }
}
