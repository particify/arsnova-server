/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Controller
@RestController
class ChallengeController(
    val challengeService: ChallengeService,
    private val reusableTokens: ReusableChallengeTokens
) {
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
            result.invalidSignature == true -> "Invalid signature"
            result.invalidSolution == true -> "Invalid solution"
            else -> "Verification failed"
          })
    }
    if (!encodedPayload.reusable) {
      return TokenResponse(challengeService.createJwtForSolution(encodedPayload.solution))
    }
    return reusableToken(encodedPayload.solution)
  }

  /**
   * The token is reusable until it expires, so the solution is consumed now rather than when a
   * token is presented: it cannot be exchanged for a second token of either kind.
   */
  private fun reusableToken(solution: String): TokenResponse {
    val id = challengeService.challengeId(solution)
    if (!challengeService.useId(id)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge solution already used")
    }
    return TokenResponse(reusableTokens.issue(id))
  }

  /**
   * @property reusable whether to issue a token which is valid for any number of requests until it
   *   expires. Otherwise, the token authorizes a single anonymous operation.
   */
  data class ChallengePayload(val solution: String, val reusable: Boolean = false)

  data class TokenResponse(val token: String)
}
