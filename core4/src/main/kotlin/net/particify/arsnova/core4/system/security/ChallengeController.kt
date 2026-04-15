/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Controller
@RestController
class ChallengeController(val challengeService: ChallengeService) {
  @GetMapping("/challenge")
  fun challenge(): String {
    return challengeService.generateChallenge().toJson()
  }

  @PostMapping("/challenge")
  fun verifyChallenge(@RequestBody encodedPayload: ChallengePayload): TokenResponse {
    val result = challengeService.verifySolution(encodedPayload.solution)
    if (!result.verified) {
      throw ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          when {
            result.expired -> "Challenge expired"
            result.invalidSignature -> "Invalid signature"
            result.invalidSolution -> "Invalid solution"
            else -> "Verification failed"
          })
    }
    return TokenResponse(challengeService.createJwtForSolution(encodedPayload.solution))
  }

  data class ChallengePayload(val solution: String)

  data class TokenResponse(val token: String)
}
