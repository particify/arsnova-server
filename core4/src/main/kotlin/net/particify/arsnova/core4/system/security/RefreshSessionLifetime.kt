/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.time.Duration
import java.time.Instant

private const val EXTEND_BY_CLAIM = "extendBy"
private const val EXTEND_UNTIL_CLAIM = "extendUntil"

/**
 * How long the session a refresh token belongs to lasts, as decided when it was started. It travels
 * within the signed token so that a rotation continues the session instead of deciding it anew.
 *
 * [extendBy] is the period a rotation slides forward, [extendUntil] the instant the session ends
 * whatever happens in between. A session bounded that way outlives neither, so the last token
 * before [extendUntil] is shortened to reach exactly that far.
 *
 * Every value is absent for a token issued before the lifetime became part of one.
 */
data class RefreshSessionLifetime(
    val extendBy: Duration? = null,
    val extendUntil: Instant? = null
) {
  /** A session which was never given a deadline does not end this way. */
  fun hasEnded() = extendUntil != null && !Instant.now().isBefore(extendUntil)

  fun toClaims(): Map<String, Any> = buildMap {
    extendBy?.let { put(EXTEND_BY_CLAIM, it.seconds) }
    extendUntil?.let { put(EXTEND_UNTIL_CLAIM, it.epochSecond) }
  }

  companion object {
    fun fromClaims(claims: Map<String, Any>) =
        RefreshSessionLifetime(
            (claims[EXTEND_BY_CLAIM] as? Number)?.let { Duration.ofSeconds(it.toLong()) },
            (claims[EXTEND_UNTIL_CLAIM] as? Number)?.let { Instant.ofEpochSecond(it.toLong()) })
  }
}
