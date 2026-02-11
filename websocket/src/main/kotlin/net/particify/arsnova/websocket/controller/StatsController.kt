package net.particify.arsnova.websocket.controller

import net.particify.arsnova.websocket.event.RoomSubscriptionEventDispatcher
import net.particify.arsnova.websocket.exception.ForbiddenException
import net.particify.arsnova.websocket.model.Stats
import net.particify.arsnova.websocket.security.JwtTokenUtil
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController("StatsController")
class StatsController(
  private val roomSubscriptionEventDispatcher: RoomSubscriptionEventDispatcher,
  private val jwtTokenUtil: JwtTokenUtil,
) {
  companion object {
    const val STATS_MAPPING = "/stats"
  }

  @GetMapping(STATS_MAPPING)
  fun getStats(
    @RequestHeader(
      name = "Authorization",
    ) bearer: String,
  ): Mono<Stats> {
    val roles = jwtTokenUtil.getRolesFromToken(bearer.removePrefix("Bearer "))
    if (!roles.contains("ADMIN")) {
      throw ForbiddenException()
    }
    return Mono.just(Stats(roomSubscriptionEventDispatcher.getWsSessionCount()))
  }
}
