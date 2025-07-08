package net.particify.arsnova.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class RestConfig(
  private val httpGatewayProperties: HttpGatewayProperties,
) {
  @Bean
  fun authServiceWebClient(): WebClient? =
    WebClient
      .builder()
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build()
}
