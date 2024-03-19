package net.particify.arsnova.authz.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig {
  companion object {
    const val SECRET_ALGORITHM = "HmacSHA256"
  }

  val logger: Logger = LoggerFactory.getLogger(this::class.java)

  @Bean
  fun jwtDecoder(authServiceProperties: AuthServiceProperties): JwtDecoder? {
    if (authServiceProperties.security.jwt.idpIssuer != null) {
      logger.debug("Setting up JWT decoder using JWKS of IdP issuer.")
      return JwtDecoders.fromIssuerLocation(authServiceProperties.security.jwt.idpIssuer)
    }

    logger.debug("Setting up JWT decoder using secret.")
    val key = SecretKeySpec(authServiceProperties.security.jwt.secret.toByteArray(), SECRET_ALGORITHM)
    return NimbusJwtDecoder.withSecretKey(key).build()
  }
}
