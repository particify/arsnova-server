package net.particify.arsnova.gateway.config

import net.particify.arsnova.gateway.security.SecurityContextRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class WebSecurityConfig(
  private val securityContextRepository: SecurityContextRepository,
) {
  @Bean
  fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? =
    http
      .exceptionHandling {
        it
          .authenticationEntryPoint { swe: ServerWebExchange, _: AuthenticationException? ->
            Mono.fromRunnable { swe.response.statusCode = HttpStatus.UNAUTHORIZED }
          }.accessDeniedHandler { swe: ServerWebExchange, _: AccessDeniedException? ->
            Mono.fromRunnable { swe.response.statusCode = HttpStatus.FORBIDDEN }
          }
      }.csrf { it.disable() }
      .formLogin { it.disable() }
      .httpBasic { it.disable() }
      .securityContextRepository(securityContextRepository)
      .authorizeExchange {
        it
          .pathMatchers(HttpMethod.OPTIONS)
          .permitAll()
          .pathMatchers("/management" + "/**")
          .hasAnyRole("ADMIN", "MONITORING")
          .pathMatchers("/**")
          .permitAll()
          .anyExchange()
          .authenticated()
      }.build()
}
