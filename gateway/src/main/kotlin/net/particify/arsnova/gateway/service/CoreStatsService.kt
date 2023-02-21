package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import net.particify.arsnova.gateway.model.CoreStats
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI

@Service
class CoreStatsService(
  private val webClient: WebClient,
  private val httpGatewayProperties: HttpGatewayProperties
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val url = URI("${httpGatewayProperties.httpClient.core}/management/stats")

  fun getServiceStats(jwt: String, params: MultiValueMap<String, String>): Mono<Map<String, Any>> {
    logger.trace("Querying core for stats with url: {}", url)
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
      .header("Authorization", jwt)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
      .cache()
      .checkpoint("Request failed in ${this::class.simpleName}::${::getServiceStats.name}.")
  }

  fun getSummarizedStats(jwt: String, params: MultiValueMap<String, String>): Mono<CoreStats> {
    logger.trace("Querying core for stats with url: {}", url)
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
      .header("Authorization", jwt)
      .retrieve()
      .bodyToMono(CoreStats::class.java)
      .cache()
      .checkpoint("Request failed in ${this::class.simpleName}::${::getSummarizedStats.name}.")
  }
}
