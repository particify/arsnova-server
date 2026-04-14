/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.altcha.altcha.v2.Altcha
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

@Service
class ChallengeService(
    securityProperties: SecurityProperties,
    val jwtUtils: JwtUtils,
    val jsonMapper: JsonMapper
) {
  val challengeProperties = securityProperties.challenge
  val secureRandom = SecureRandom()
  @Volatile var freshBlockedIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
  @Volatile var staleBlockedIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

  fun generateChallenge(): Altcha.Challenge {
    val counter = this.secureRandom.nextInt(this.challengeProperties.maxIterations)
    val options =
        Altcha.CreateChallengeOptions()
            .algorithm(this.challengeProperties.algorithm)
            .cost(this.challengeProperties.iterationCost)
            .counter(counter)
            .hmacSignatureSecret(this.challengeProperties.secret)
            .expiresInSeconds(this.challengeProperties.validitySeconds)
            .data(mapOf("id" to UUID.randomUUID().toString()))
    return Altcha.createChallenge(options)
  }

  fun verifySolution(solution: String): Altcha.VerifySolutionResult {
    val payload = parsePayload(solution)
    return Altcha.verifySolution(
        payload.challenge,
        payload.solution,
        this.challengeProperties.secret,
        Altcha.kdf(this.challengeProperties.algorithm))
  }

  fun createJwtForSolution(solution: String): String {
    val payload = parsePayload(solution)
    val id = payload.challenge.parameters.data["id"]
    val expires = Instant.ofEpochSecond(payload.challenge.parameters.expiresAt)
    return jwtUtils.encodeJwt(
        "challenge-$id", listOf(CHALLENGE_SOLVED_ROLE), expirationTime = expires)
  }

  private fun parsePayload(solution: String): Altcha.Payload {
    return jsonMapper.readValue(Base64.getDecoder().decode(solution), Altcha.Payload::class.java)
  }

  fun useId(id: UUID): Boolean {
    return !staleBlockedIds.contains(id) && freshBlockedIds.add(id)
  }

  /**
   * Clears the list of stale IDs and swaps it with the list of fresh IDs.
   *
   * ID blocking uses two separate lists, one for fresh IDs, which is used for inserting, and one
   * for stale IDs. These lists are swapped after a fixed interval. While IDs might be stored longer
   * than necessary, we do not need to keep track of individual expiration times.
   */
  @Scheduled(
      fixedRateString = $$"${security.challenge.validity-seconds}", timeUnit = TimeUnit.SECONDS)
  fun clearStaleBlockedIds() {
    staleBlockedIds.clear()
    val swap = freshBlockedIds
    freshBlockedIds = staleBlockedIds
    staleBlockedIds = swap
  }
}
