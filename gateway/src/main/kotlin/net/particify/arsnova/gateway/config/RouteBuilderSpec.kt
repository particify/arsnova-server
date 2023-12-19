package net.particify.arsnova.gateway.config

import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.route.builder.Buildable
import org.springframework.cloud.gateway.route.builder.PredicateSpec

data class RouteBuilderSpec(
  val id: String,
  val fn: (PredicateSpec) -> Buildable<Route>,
)
