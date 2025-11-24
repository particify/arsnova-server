/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.text.toLong
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.altcha.altcha.Altcha
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

@Service
class ChallengeService(
    securityProperties: SecurityProperties,
    val jwtUtils: JwtUtils,
    val objectMapper: JsonMapper
) {
  val challengeProperties = securityProperties.challenge
  @Volatile var freshBlockedIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
  @Volatile var staleBlockedIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

  fun generateChallenge(): Altcha.Challenge {
    val options = Altcha.ChallengeOptions()
    options.hmacKey = this.challengeProperties.secret
    options.maxNumber = this.challengeProperties.maxNumber
    options.expires =
        Instant.now().plusSeconds(this.challengeProperties.validitySeconds).epochSecond
    options.params["id"] = UUID.randomUUID().toString()
    return Altcha.createChallenge(options)
  }

  fun verifySolution(solution: String): Boolean {
    return Altcha.verifySolution(solution, this.challengeProperties.secret, true)
  }

  fun createJwtForSolution(solution: String): String {
    val payload =
        objectMapper.readValue(Base64.getDecoder().decode(solution), Altcha.Payload::class.java)
    val id = Altcha.extractParams(payload.salt)["id"]
    val expires = Instant.ofEpochSecond(Altcha.extractParams(payload.salt)["expires"]!!.toLong())

    return jwtUtils.encodeJwt(
        "challenge-$id", listOf(CHALLENGE_SOLVED_ROLE), expirationTime = expires)
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
