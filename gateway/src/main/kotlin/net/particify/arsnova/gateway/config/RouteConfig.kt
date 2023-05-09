package net.particify.arsnova.gateway.config

import net.particify.arsnova.gateway.filter.AddMembershipFilter
import net.particify.arsnova.gateway.filter.AddRoomCreatorAccessFilter
import net.particify.arsnova.gateway.filter.AuthFilter
import net.particify.arsnova.gateway.filter.JwtUserIdFilter
import net.particify.arsnova.gateway.filter.RemoveMembershipFilter
import net.particify.arsnova.gateway.filter.RequestRateLimiter
import net.particify.arsnova.gateway.filter.RoomIdFilter
import net.particify.arsnova.gateway.filter.RoomShortIdFilter
import net.particify.arsnova.gateway.filter.UpdateRoomAccessFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.handler.predicate.RemoteAddrRoutePredicateFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(
  protected val httpGatewayProperties: HttpGatewayProperties,
  protected val requestRateLimiter: RequestRateLimiter
) {
  companion object {
    const val UTIL_PREFIX = "/_util"

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun routeLocator(
    builder: RouteLocatorBuilder,
    routeBuilderSpecs: List<List<RouteBuilderSpec>>
  ): RouteLocator {
    val routes = builder.routes()
    val specs = routeBuilderSpecs.flatten()

    logger.debug("Creating routes for IDs: {}", specs.map { spec -> spec.id })

    for (spec in specs) {
      routes.route(spec.id, spec.fn)
    }

    return routes.build()
  }

  @Bean
  fun routes(
    authFilter: AuthFilter,
    addMembershipFilter: AddMembershipFilter,
    removeMembershipFilter: RemoveMembershipFilter,
    roomIdFilter: RoomIdFilter,
    roomShortIdFilter: RoomShortIdFilter,
    roomAuthFilter: UpdateRoomAccessFilter,
    roomCreationAuthFilter: AddRoomCreatorAccessFilter,
    jwtUserIdFilter: JwtUserIdFilter
  ): List<RouteBuilderSpec> {
    var routes = listOf(
      RouteBuilderSpec("healthz") { p ->
        p
          .path("/healthz")
          .and()
          .predicate(
            RemoteAddrRoutePredicateFactory().apply(
              RemoteAddrRoutePredicateFactory.Config().setSources(
                httpGatewayProperties.gateway.healthzAllowedIpAddresses
              )
            )
          )
          .filters { f ->
            f.rewritePath("^/healthz", "/management/health")
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("comments") { p ->
        p
          .path(
            "/room/{roomId}/comment/**",
            "/room/{roomId}/settings/**",
            "/room/{roomId}/vote/**"
          )
          .filters { f ->
            f.filter(authFilter.apply(AuthFilter.Config()))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.commentService)
      },

      RouteBuilderSpec("request-membership") { p ->
        p
          .path("/room/{roomId}/request-membership")
          .filters { f ->
            f.filter(addMembershipFilter.apply(AddMembershipFilter.Config()))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("cancel-membership") { p ->
        p
          .path("/room/{roomId}/cancel-membership")
          .filters { f ->
            f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
            f.filter(authFilter.apply(AuthFilter.Config()))
            f.filter(removeMembershipFilter.apply(RemoveMembershipFilter.Config()))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("contents") { p ->
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
      },

      RouteBuilderSpec("rooms-duplicate") { p ->
        p
          .path("/room/{roomId}/duplicate")
          .filters { f ->
            f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
            f.filter(authFilter.apply(AuthFilter.Config(false)))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("rooms-post") { p ->
        p
          .path("/room/")
          .filters { f ->
            f.filter(roomCreationAuthFilter.apply(AddRoomCreatorAccessFilter.Config()))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("rooms-access") { p ->
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
      },

      RouteBuilderSpec("rooms") { p ->
        p
          .path("/room/{roomId}/**")
          .filters { f ->
            f.filter(roomShortIdFilter.apply(RoomShortIdFilter.Config()))
            f.filter(authFilter.apply(AuthFilter.Config()))
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("core-without-room-auth") { p ->
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
      },

      RouteBuilderSpec("formatting") { p ->
        p
          .path("$UTIL_PREFIX/formatting/render")
          .filters { f ->
            f.rewritePath("^$UTIL_PREFIX/formatting", "")
            f.requestRateLimiter { r ->
              r.rateLimiter = requestRateLimiter
            }
          }
          .uri(httpGatewayProperties.routing.endpoints.formattingService)
      },

      RouteBuilderSpec("management-core") { p ->
        p
          .path("/management/core/**")
          .filters { f ->
            f.rewritePath("^/management/core", "/management")
          }
          .uri(httpGatewayProperties.routing.endpoints.core)
      },

      RouteBuilderSpec("management-websocket") { p ->
        p
          .path("/management/ws-gateway/**")
          .filters { f ->
            f.rewritePath("^/management/ws-gateway", "/management")
          }
          .uri(httpGatewayProperties.routing.endpoints.wsGateway)
      },

      RouteBuilderSpec("management-comments") { p ->
        p
          .path("/management/comment-service/**")
          .filters { f ->
            f.rewritePath("^/management/comment-service", "/management")
          }
          .uri(httpGatewayProperties.routing.endpoints.commentService)
      },

      RouteBuilderSpec("management-authz") { p ->
        p
          .path("/management/auth-service/**")
          .filters { f ->
            f.rewritePath("^/management/auth-service", "/management")
          }
          .uri(httpGatewayProperties.routing.endpoints.roomaccessService)
      }
    )

    if (httpGatewayProperties.routing.endpoints.proxyMetrics != null) {
      routes = routes.plus(
        RouteBuilderSpec("metrics-proxy") { p ->
          p
            .path("/management/proxy/prometheus")
            .filters { f ->
              f.rewritePath("^/management/proxy", "")
            }
            .uri(httpGatewayProperties.routing.endpoints.proxyMetrics)
        }
      )
    }

    return routes
  }
}
