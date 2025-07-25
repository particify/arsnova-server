package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class ContentService(
  private val webClient: WebClient,
  private val httpGatewayProperties: HttpGatewayProperties,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getStats(
    roomIds: List<String>,
    jwt: String,
  ): Flux<Int> {
    val url = "${httpGatewayProperties.httpClient.core}/room/-/content/-/count?roomIds=${roomIds.joinToString(",")}"
    logger.trace("Querying core for content stats with url: {}", url)
    return webClient
      .get()
      .uri(url)
      .header("Authorization", jwt)
      .retrieve()
      .bodyToFlux(Int::class.java)
      .cache()
      .checkpoint("Request failed in ${this::class.simpleName}::${::getStats.name}.")
  }
}
