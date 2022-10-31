package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import net.particify.arsnova.gateway.model.CoreStats
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CoreStatsService(
  private val webClient: WebClient,
  private val httpGatewayProperties: HttpGatewayProperties
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getServiceStats(jwt: String): Mono<Map<String, Any>> {
    val url = "${httpGatewayProperties.httpClient.core}/management/stats"
    logger.trace("Querying core for stats with url: {}", url)
    return webClient.get()
      .uri(url)
      .header("Authorization", jwt)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
      .cache()
      .checkpoint("Request failed in ${this::class.simpleName}::${::getServiceStats.name}.")
  }

  fun getSummarizedStats(jwt: String): Mono<CoreStats> {
    val url = "${httpGatewayProperties.httpClient.core}/management/stats"
    logger.trace("Querying core for stats with url: {}", url)
    return webClient.get()
      .uri(url)
      .header("Authorization", jwt)
      .retrieve()
      .bodyToMono(CoreStats::class.java)
      .cache()
      .checkpoint("Request failed in ${this::class.simpleName}::${::getSummarizedStats.name}.")
  }
}
