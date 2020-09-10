package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.util.Optional

@Service
class WsGatewayService(
        private val webClient: WebClient,
        private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getUsercount(roomIds: List<String>): Flux<Optional<Int>> {
        val url = "${httpGatewayProperties.httpClient.wsGateway}/roomsubscription/usercount?ids=${roomIds.joinToString(",")}"
        val typeRef: ParameterizedTypeReference<List<Int?>> = object : ParameterizedTypeReference<List<Int?>>() {}
        return webClient.get()
                .uri(url)
                .retrieve().bodyToMono(typeRef)
                .flatMapMany { userCounts: List<Int?> ->
                    Flux.fromIterable(userCounts.map { entry ->
                        if (entry != null) {
                            Optional.of(entry)
                        } else {
                            Optional.empty()
                        }
                    })
                }
                .onErrorResume { exception ->
                    logger.debug("Exception on getting room subscription user count from ws gw", exception)
                    Flux.fromIterable(roomIds.map { id ->
                        // using a local var for this is needed because otherwise type can't be interfered
                        val h: Optional<Int> = Optional.empty()
                        h
                    })
                }
    }
}
