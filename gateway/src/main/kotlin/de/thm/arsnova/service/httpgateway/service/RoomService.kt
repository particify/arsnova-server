package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.Room
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional


@Service
class RoomService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun get(roomId: String): Mono<Room> {
        val url = "${httpGatewayProperties.httpClient.core}/room/$roomId"
        logger.trace("Querying core for room with url: {}", url)
        return webClient.get().uri(url)
            .retrieve().bodyToMono(Room::class.java).cache()
                .onErrorResume { exception ->
                    logger.debug("Error on getting room with id: {}", roomId, exception)
                    Mono.empty()
                }
    }

    fun get(roomIds: List<String>): Flux<Optional<Room>> {
        val path = "${httpGatewayProperties.httpClient.core}/room/"
        val url = "${path}?ids=${roomIds.joinToString(",")}&skipMissing=false"
        logger.trace("Querying core for room with url: {}", url)
        val typeRef: ParameterizedTypeReference<List<Room?>> = object : ParameterizedTypeReference<List<Room?>>() {}
        return webClient.get().uri(url)
            .retrieve().bodyToMono(typeRef).cache()
                .flatMapMany { roomList: List<Room?> ->
                    Flux.fromIterable(roomList.map { entry ->
                        if (entry != null) {
                            Optional.of(Room(entry.id, entry.shortId, entry.name))
                        } else {
                            Optional.empty()
                        }
                    })
                }
    }

    fun getByShortId(shortId: String): Mono<Room> {
        val path = "${httpGatewayProperties.httpClient.core}/room/"
        val url = "${path}~${shortId}"
        logger.trace("Querying core for room by shortId with url: {}", url)
        return webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(Room::class.java)
    }
}
