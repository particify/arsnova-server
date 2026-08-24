/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.springframework.stereotype.Service

private const val LOCAL_PROVIDER_KEY = "local"

/**
 * Limits login attempts per authentication provider and submitted username. Without this, the login
 * endpoint can be used as a password oracle against an external directory, where repeated failures
 * usually lock the account.
 *
 * The counters are held in memory, so the limit applies per instance and not to a deployment with
 * multiple replicas.
 */
@Service
class LoginAttemptService(securityProperties: SecurityProperties) {
  private val attemptLimit = securityProperties.login.attemptLimit
  private val attemptWindow = securityProperties.login.attemptWindow
  private val buckets: MutableMap<String, Bucket> = ConcurrentHashMap()

  /** Consumes one attempt and returns false if none are left for the current window. */
  fun tryConsumeAttempt(providerId: UUID?, username: String): Boolean {
    val bucket = buckets.computeIfAbsent(key(providerId, username)) { createBucket() }
    return bucket.tryConsume(1)
  }

  /** Discards the consumed attempts, to be called after a successful authentication. */
  fun resetAttempts(providerId: UUID?, username: String) {
    buckets.remove(key(providerId, username))
  }

  private fun createBucket(): Bucket {
    val bandwidth =
        BandwidthBuilder.builder()
            .capacity(attemptLimit)
            .refillIntervally(attemptLimit, attemptWindow)
            .build()
    return Bucket.builder().addLimit(bandwidth).build()
  }

  private fun key(providerId: UUID?, username: String): String =
      "${providerId?.toString() ?: LOCAL_PROVIDER_KEY}:$username"
}
