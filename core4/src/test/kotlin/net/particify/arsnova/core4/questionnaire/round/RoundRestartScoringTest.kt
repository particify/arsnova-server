/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.time.Instant
import net.particify.arsnova.core4.questionnaire.content.Content
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val NOW = Instant.parse("2026-01-01T00:00:00Z")
private const val DURATION_SECONDS = 30
private const val BASE_POINTS = 500

/**
 * Restarting a content whose countdown had run out used to leave the round without a deadline,
 * which made answering time unmeasurable and handed out the unweighted score to everyone.
 */
class RoundRestartScoringTest {
  private val content = Content(duration = DURATION_SECONDS, points = BASE_POINTS)

  private fun startedRound(): Round {
    val round = Round(content = content)
    round.startAnswering(DURATION_SECONDS, NOW)
    return round
  }

  @Test
  fun shouldMeasureNoElapsedTimeForAnInstantAnswer() {
    Assertions.assertEquals(0, startedRound().elapsedAnsweringTime(NOW))
  }

  @Test
  fun shouldMeasureElapsedTimeWhileTheCountdownRuns() {
    Assertions.assertEquals(10_000, startedRound().elapsedAnsweringTime(NOW.plusSeconds(10)))
  }

  @Test
  fun shouldNotMeasureElapsedTimeWithoutACountdown() {
    val round = Round(content = Content())
    round.startAnswering(null, NOW)
    Assertions.assertNull(round.elapsedAnsweringTime(NOW))
  }

  @Test
  fun shouldMeasureAnsweringTimeAgainstTheRestartedCountdown() {
    val round = startedRound()
    val restartTime = NOW.plusSeconds(DURATION_SECONDS.toLong() + 20)
    round.openAnswering(DURATION_SECONDS, restartTime)
    Assertions.assertEquals(0, round.elapsedAnsweringTime(restartTime))
    Assertions.assertEquals(10_000, round.elapsedAnsweringTime(restartTime.plusSeconds(10)))
  }
}
