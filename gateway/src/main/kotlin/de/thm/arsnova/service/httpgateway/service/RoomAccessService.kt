package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RoomAccessService(
        private val webClient: WebClient,
        private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(RoomAccessService::class.java)

    fun getRoomAccess(roomId: String, userId: String): Mono<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/$roomId/$userId"
        logger.trace("Querying auth service for room access with url: {}", url)
        return webClient.get().uri(url)
                .retrieve().bodyToMono(RoomAccess::class.java).cache()
    }

    fun postRoomAccess(roomAccess: RoomAccess): Mono<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/"
        logger.trace("Posting to auth service for room access with url: {}, roomAccess: {}", url, roomAccess)
        return webClient.post().uri(url)
            .body(BodyInserters.fromPublisher(Mono.just(roomAccess), RoomAccess::class.java))
            .retrieve()
            .bodyToMono(RoomAccess::class.java)
    }

    fun postRoomAccessWithLimit(roomAccess: RoomAccess, roomParticipantLimit: Int): Mono<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/?roomParticipantLimit=${roomParticipantLimit}"
        logger.trace("Posting to auth service for room access with url: {}, roomAccess: {}", url, roomAccess)
        return webClient.post().uri(url)
            .body(BodyInserters.fromPublisher(Mono.just(roomAccess), RoomAccess::class.java))
            .retrieve()
            .bodyToMono(RoomAccess::class.java)
            .onErrorResume { e ->
                if (e is WebClientResponseException.Forbidden) {
                    Mono.error(ForbiddenException())
                } else {
                    Mono.error(e)
                }
            }
    }

    fun deleteRoomAccess(roomAccess: RoomAccess): Mono<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/${roomAccess.roomId}/${roomAccess.userId}"
        logger.trace("Deleting to auth service for room access with url: {}, roomAccess: {}", url, roomAccess)
        return webClient.delete().uri(url)
            .retrieve()
            .bodyToMono(RoomAccess::class.java)
    }

    fun deleteRoomAccessByRoomId(roomId: String): Flux<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/${roomId}"
        logger.trace("Deleting to auth service for room access with url: {}", url)
        return webClient.delete().uri(url)
            .retrieve()
            .bodyToFlux(RoomAccess::class.java)
    }

    fun getRoomAccessByUser(userId: String): Flux<RoomAccess> {
        val url = "${httpGatewayProperties.httpClient.authService}/roomaccess/by-user/$userId"
        logger.trace("Querying auth service for room access with url: {}", url)
        return webClient.get().uri(url)
                .retrieve().bodyToFlux(RoomAccess::class.java).cache()
    }
}
