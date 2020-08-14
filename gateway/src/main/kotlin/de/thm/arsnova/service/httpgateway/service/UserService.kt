package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun get(userId: String, jwt: String): Mono<User> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId?view=owner"
        logger.trace("Querying core for user with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToMono(User::class.java).cache()
    }

    fun getRoomHistory(userId: String, jwt: String): Flux<RoomHistoryEntry> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId/roomHistory"
        logger.trace("Querying core for room history with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToFlux(RoomHistoryEntry::class.java).cache()
    }
}
