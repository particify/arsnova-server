/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import org.altcha.altcha.Altcha
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Controller
@RestController
class ChallengeController(val challengeService: ChallengeService) {
  @GetMapping("/challenge")
  fun challenge(): Altcha.Challenge {
    return challengeService.generateChallenge()
  }

  @PostMapping("/challenge")
  fun verifyChallenge(@RequestBody encodedPayload: ChallengePayload): TokenResponse {
    if (!challengeService.verifySolution(encodedPayload.solution))
        throw InvalidChallengeSolutionException()
    return TokenResponse(challengeService.createJwtForSolution(encodedPayload.solution))
  }

  data class ChallengePayload(val solution: String)

  data class TokenResponse(val token: String)

  class InvalidChallengeSolutionException : Exception("Invalid challenge solution")
}
