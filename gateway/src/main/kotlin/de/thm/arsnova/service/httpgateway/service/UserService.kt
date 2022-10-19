package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

@Service
class UserService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties,
    private val authProcessor: AuthProcessor
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun get(userId: String, jwt: String): Mono<User> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId?view=owner"
        logger.trace("Querying core for user with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToMono(User::class.java).cache()
            .checkpoint("Request failed in ${this::class.simpleName}::${::get.name}.")
    }

    fun exists(userId: String, jwt: String): Mono<Boolean> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId"
        logger.trace("Querying core for user with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToMono(User::class.java).cache()
            .map { true }
            .onErrorResume { e ->
                if (e !is NotFound) {
                    throw e
                }
                Mono.just(false)
            }
    }

    fun getRoomHistory(userId: String, jwt: String): Flux<RoomHistoryEntry> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId/roomHistory"
        logger.trace("Querying core for room history with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToFlux(RoomHistoryEntry::class.java).cache()
            .checkpoint("Request failed in ${this::class.simpleName}::${::getRoomHistory.name}.")
    }

    fun updateAnnouncementReadTimestamp(userId: String, jwt: String): Mono<ResponseEntity<Void>> {
        val url = "${httpGatewayProperties.httpClient.core}/user/$userId/"
        return webClient.patch()
            .uri(url)
            .header("Authorization", jwt)
            .bodyValue(AnnouncementReadTimestamp(Date()))
            .retrieve()
            .toBodilessEntity()
            .checkpoint("Request failed in ${this::class.simpleName}::${::getRoomHistory.name}.")
    }

    data class AnnouncementReadTimestamp(val announcementReadTimestamp: Date)
}
