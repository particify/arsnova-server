package de.thm.arsnova.service.wsgateway.controller

import de.thm.arsnova.service.wsgateway.event.RoomSubscriptionEventDispatcher
import de.thm.arsnova.service.wsgateway.model.Stats
import de.thm.arsnova.service.wsgateway.service.RoomSubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController("StatsController")
class StatsController(
		private val roomSubscriptionEventDispatcher: RoomSubscriptionEventDispatcher,
) {
	companion object {
		const val STATS_MAPPING = "/stats"
	}

	private val logger = LoggerFactory.getLogger(this::class.java)

	@GetMapping(STATS_MAPPING)
	fun getStats(): Mono<Stats> {
		return Mono.just(Stats(roomSubscriptionEventDispatcher.getWsSessionCount()))
	}
}
