package net.particify.arsnova.gateway.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
@EnableConfigurationProperties(HttpGatewayProperties::class)
class GatewayConfig {
  @Bean
  fun ipKeyResolver(): KeyResolver {
    return KeyResolver { exchange ->
      Mono.just(
        listOf(
          exchange.request.method.toString(),
          exchange.request.remoteAddress!!.address.hostAddress
        )
          .joinToString(",")
      )
    }
  }
}
