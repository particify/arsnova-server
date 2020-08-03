package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.CommentStats
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CommentService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getStats(roomIds: List<String>): Flux<CommentStats> {
        return Flux
            .fromIterable(roomIds)
            .map { roomId ->
                CommentStats(roomId)
            }
    }

    fun getAckCount(roomId: String): Mono<Int> {
        return Mono.just(0)
    }

    fun getUnackCount(roomId: String): Mono<Int> {
        return Mono.just(0)
    }
}
