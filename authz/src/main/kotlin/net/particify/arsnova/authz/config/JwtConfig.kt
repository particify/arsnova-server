package net.particify.arsnova.authz.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders

@Configuration
class JwtConfig {
  val logger = LoggerFactory.getLogger(this::class.java)

  @Bean
  fun jwtDecoder(authServiceProperties: AuthServiceProperties): JwtDecoder? {
    if (authServiceProperties.security.jwt.idpIssuer == null) {
      logger.debug("IdP issuer is not set.")
      return null
    }
    logger.debug("Setting up JWT decoder for IdP issuer.")
    return JwtDecoders.fromIssuerLocation(authServiceProperties.security.jwt.idpIssuer)
  }
}
