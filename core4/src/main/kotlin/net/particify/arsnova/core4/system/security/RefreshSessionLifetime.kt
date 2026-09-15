/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.time.Duration

private const val EXTEND_BY_CLAIM = "extendBy"

/**
 * How long the session a refresh token belongs to lasts, as decided when it was started. It travels
 * within the signed token so that a rotation continues the session instead of deciding it anew.
 *
 * Every value is absent for a token issued before the lifetime became part of one.
 */
data class RefreshSessionLifetime(val extendBy: Duration? = null) {
  fun toClaims(): Map<String, Any> =
      extendBy?.let { mapOf(EXTEND_BY_CLAIM to it.seconds) } ?: mapOf()

  companion object {
    fun fromClaims(claims: Map<String, Any>) =
        RefreshSessionLifetime(
            (claims[EXTEND_BY_CLAIM] as? Number)?.let { Duration.ofSeconds(it.toLong()) })
  }
}
