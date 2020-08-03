package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.Room
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class RoomService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun get(roomId: String): Mono<Room> {
        val url = "${httpGatewayProperties.httpClient!!.core}/$roomId"
        logger.trace("Querying core for room with url: {}", url)
        return webClient.get().uri(url)
            .retrieve().bodyToMono(Room::class.java).cache()
    }
}
