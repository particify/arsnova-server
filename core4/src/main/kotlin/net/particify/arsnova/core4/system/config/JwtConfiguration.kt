/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import javax.crypto.spec.SecretKeySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
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
      return JwtDecoders.fromIssuerLocation(securityProperties.jwt.idpIssuer.toString())
    }
    logger.debug("Setting up JWT decoder using secret.")
    val key = SecretKeySpec(securityProperties.jwt.secret.toByteArray(), SECRET_ALGORITHM)
    return NimbusJwtDecoder.withSecretKey(key).build()
  }
}
