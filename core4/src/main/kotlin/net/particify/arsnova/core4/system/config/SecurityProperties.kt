/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.net.URL
import java.time.Duration
import org.hibernate.validator.constraints.Length
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "security")
@Validated
data class SecurityProperties(
    @field:Valid val jwt: Jwt,
    @field:Valid val challenge: Challenge,
    @field:NotBlank val authorizeUriHeader: String,
    @field:NotBlank val authorizeUriPrefix: String,
) {
  data class Jwt(
      @field:Length(min = 32) val secret: String,
      @field:NotBlank val issuer: String,
      val idpIssuer: URL?,
      val validityPeriod: Duration
  )

  data class Challenge(
      @field:Length(min = 32) val secret: String,
      val maxNumber: Long,
      val validitySeconds: Long
  )
}
