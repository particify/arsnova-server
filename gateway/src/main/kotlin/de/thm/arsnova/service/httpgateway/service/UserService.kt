package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class UserService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getRoomHistory(userId: String): Flux<RoomHistoryEntry> {
        val url = "${httpGatewayProperties.httpClient!!.core}/user/$userId/roomHistory"
        logger.trace("Querying core for room history with url: {}", url)
        return webClient.get().uri(url)
            .retrieve().bodyToFlux(RoomHistoryEntry::class.java).cache()
    }
}
