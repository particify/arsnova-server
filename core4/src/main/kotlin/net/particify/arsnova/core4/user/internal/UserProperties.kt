/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Duration
import java.time.temporal.ChronoUnit
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit

@ConfigurationProperties("user")
data class UserProperties(val inactivityThresholds: InactivityThresholds) {
  data class InactivityThresholds(
      @param:DurationUnit(ChronoUnit.DAYS) val unverified: Duration? = null,
      @param:DurationUnit(ChronoUnit.DAYS) val unverifiedSingleVisit: Duration? = null,
      @param:DurationUnit(ChronoUnit.DAYS) val verified: Duration? = null
  )
}
