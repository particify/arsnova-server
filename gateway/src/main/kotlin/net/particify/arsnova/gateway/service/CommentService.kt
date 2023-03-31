package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import net.particify.arsnova.gateway.model.CommentServiceStats
import net.particify.arsnova.gateway.model.CommentStats
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

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
      .checkpoint("Request failed in ${this::class.simpleName}::${::getStats.name}.")
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

  fun getServiceStats(params: MultiValueMap<String, String>): Mono<CommentServiceStats> {
    val url = URI("${httpGatewayProperties.httpClient.commentService}/stats")
    logger.trace("Querying comment service for stats with url: {}", url)
    return webClient.get()
      .uri {
        it
          .scheme(url.scheme)
          .host(url.host)
          .port(url.port)
          .path(url.path)
          .replaceQueryParams(params)
          .build()
      }
      .retrieve().bodyToMono(CommentServiceStats::class.java)
      .checkpoint("Request failed in ${this::class.simpleName}::${::getServiceStats.name}.")
  }

  fun getAckCount(roomId: String): Mono<Int> {
    return Mono.just(0)
  }

  fun getUnackCount(roomId: String): Mono<Int> {
    return Mono.just(0)
  }
}
