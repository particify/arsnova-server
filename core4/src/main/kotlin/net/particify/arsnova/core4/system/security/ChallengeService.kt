/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.text.toLong
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.altcha.altcha.Altcha
import org.springframework.stereotype.Service

@Service
class ChallengeService(
    securityProperties: SecurityProperties,
    val jwtUtils: JwtUtils,
    val objectMapper: ObjectMapper
) {
  val challengeProperties = securityProperties.challenge

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

    return jwtUtils.encodeJwt("challenge-$id", listOf(CHALLENGE_SOLVED_ROLE), expires)
  }
}
