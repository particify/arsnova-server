package de.thm.arsnova.service.wsgateway.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestConfig {

  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate? {
    return builder.build()
  }
}
