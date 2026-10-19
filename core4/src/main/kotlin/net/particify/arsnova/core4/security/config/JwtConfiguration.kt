/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.config

import javax.crypto.spec.SecretKeySpec
import net.particify.arsnova.core4.security.SecurityProperties
import net.particify.arsnova.core4.security.internal.JWT_CLOCK_SKEW
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
class JwtConfiguration {
  companion object {
    const val SECRET_ALGORITHM = "HmacSHA256"
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun jwtDecoder(securityProperties: SecurityProperties): JwtDecoder? {
    if (securityProperties.jwt.idpIssuer != null) {
      logger.debug("Setting up JWT decoder using JWKS of IdP issuer.")
      val issuer = securityProperties.jwt.idpIssuer.toString()
      val decoder = JwtDecoders.fromIssuerLocation<NimbusJwtDecoder>(issuer)
      decoder.setJwtValidator(
          JwtValidators.createDefaultWithValidators(
              JwtIssuerValidator(issuer), JwtTimestampValidator(JWT_CLOCK_SKEW)))
      return decoder
    }
    logger.debug("Setting up JWT decoder using secret.")
    val key = SecretKeySpec(securityProperties.jwt.secret.toByteArray(), SECRET_ALGORITHM)
    val decoder = NimbusJwtDecoder.withSecretKey(key).build()
    decoder.setJwtValidator(
        JwtValidators.createDefaultWithValidators(JwtTimestampValidator(JWT_CLOCK_SKEW)))
    return decoder
  }
}
