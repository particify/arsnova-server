package net.particify.arsnova.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper

@Configuration
class RestConfig(
  private val httpGatewayProperties: HttpGatewayProperties,
) {
  @Bean
  fun authServiceWebClient(): WebClient? =
    WebClient
      .builder()
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .codecs { configurer ->
        // The backend services expect dates as epoch milliseconds, which Jackson 3 no longer writes by default.
        configurer
          .defaultCodecs()
          .jacksonJsonEncoder(JacksonJsonEncoder(JsonMapper.builder().enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)))
      }.build()
}
