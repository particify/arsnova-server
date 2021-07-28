package de.thm.arsnova.service.httpgateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class RestConfig(
    private val httpGatewayProperties: HttpGatewayProperties
) {

    @Bean
    fun authServiceWebClient(): WebClient? {
        return WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
