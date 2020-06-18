package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.model.RoomAccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class RoomAccessService(
        private val webClient: WebClient
) {
    companion object {
        val roomAccessString = "/roomaccess"
    }

    private val logger = LoggerFactory.getLogger(RoomAccessService::class.java)

    fun getRoomAccess(roomId: String, userId: String): Mono<RoomAccess> {
        val url = "$roomAccessString/$roomId/$userId"
        logger.trace("Querying auth service for room access with url: {}", url)
        return webClient.get().uri(url)
                .retrieve().bodyToMono(RoomAccess::class.java).cache()
    }
}
