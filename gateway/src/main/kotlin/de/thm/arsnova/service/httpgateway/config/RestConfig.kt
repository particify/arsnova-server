package de.thm.arsnova.service.httpgateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.Collections


@Configuration
class RestConfig(
        private val httpGatewayProperties: HttpGatewayProperties
) {

    @Bean
    fun authServiceWebClient(): WebClient? {
        return WebClient
                .builder()
                .baseUrl(httpGatewayProperties.httpClient!!.authService)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
                .build();
    }
}
