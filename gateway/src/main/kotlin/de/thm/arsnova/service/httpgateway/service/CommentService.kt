package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.CommentServiceStats
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

    fun getStats(roomIds: List<String>, jwt: String): Flux<CommentStats> {
        val url = "${httpGatewayProperties.httpClient.commentService}/stats/comment-stats-by-rooms?roomIds=${roomIds.joinToString(",")}"
        logger.trace("Querying comment service for comment stats with url: {}", url)
        return webClient.get()
            .uri(url)
            .header("Authorization", jwt)
            .retrieve().bodyToFlux(CommentStats::class.java).cache()
            .onErrorResume { exception ->
                logger.debug("Error on getting stats from comment service", exception)
                Flux.fromIterable(
                    roomIds.map { roomId ->
                        CommentStats(
                            roomId,
                            null
                        )
                    }
                )
            }
    }

    fun getServiceStats(): Mono<CommentServiceStats> {
        val url = "${httpGatewayProperties.httpClient.commentService}/stats"
        logger.trace("Querying comment service for stats with url: {}", url)
        return webClient.get()
            .uri(url)
            .retrieve().bodyToMono(CommentServiceStats::class.java)
    }

    fun getAckCount(roomId: String): Mono<Int> {
        return Mono.just(0)
    }

    fun getUnackCount(roomId: String): Mono<Int> {
        return Mono.just(0)
    }
}
