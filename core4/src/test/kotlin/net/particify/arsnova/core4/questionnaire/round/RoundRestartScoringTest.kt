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
 * which made answering time unmeasurable and handed out the unweighted score to everyone. These
 * cover the whole chain from the countdown to the points it produces. Answers given after the
 * restart are timed against the new countdown but earn no points.
 */
class RoundRestartScoringTest {
  private val content = Content(duration = DURATION_SECONDS, points = BASE_POINTS)

  private fun startedRound(): Round {
    val round = Round(content = content)
    round.startAnswering(DURATION_SECONDS, NOW)
    return round
  }

  private fun pointsFor(round: Round, at: Instant) =
      content.calculateCompetitivePoints(BASE_POINTS, round.elapsedAnsweringTime(at))

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

  @Test
  fun shouldNotScoreAnswersGivenAfterTheRestart() {
    val round = startedRound()
    val restartTime = NOW.plusSeconds(DURATION_SECONDS.toLong() + 20)
    round.openAnswering(DURATION_SECONDS, restartTime)
    Assertions.assertTrue(round.scoresAnswerAt(NOW.plusSeconds(10)))
    Assertions.assertFalse(round.scoresAnswerAt(restartTime.plusSeconds(10)))
  }

  @Test
  fun shouldKeepScoringAfterAStopWithTimeLeft() {
    val round = startedRound()
    round.stopAnswering(NOW.plusSeconds(10))
    val reopenTime = NOW.plusSeconds(DURATION_SECONDS.toLong() + 20)
    round.openAnswering(DURATION_SECONDS, reopenTime)
    Assertions.assertNull(round.answeringExpiredAt)
    Assertions.assertTrue(round.scoresAnswerAt(reopenTime.plusSeconds(10)))
  }

  @Test
  fun shouldWeightPointsByAnsweringSpeedAfterARestart() {
    val round = startedRound()
    val restartTime = NOW.plusSeconds(DURATION_SECONDS.toLong() + 20)
    round.openAnswering(DURATION_SECONDS, restartTime)
    // An instant answer is worth double, one at half time the plain value, one at the buzzer none.
    Assertions.assertEquals(2 * BASE_POINTS, pointsFor(round, restartTime))
    Assertions.assertEquals(BASE_POINTS, pointsFor(round, restartTime.plusSeconds(15)))
    Assertions.assertEquals(0, pointsFor(round, restartTime.plusSeconds(DURATION_SECONDS.toLong())))
  }

  @Test
  fun shouldWeightPointsByTheRemainderAfterResumingAStoppedCountdown() {
    val round = startedRound()
    round.stopAnswering(NOW.plusSeconds(20)) // 10s left
    val resumeTime = NOW.plusSeconds(25)
    round.openAnswering(DURATION_SECONDS, resumeTime)
    // 20s of the countdown were already used up, so answering now is worth two thirds of the base
    // value rather than the double an answer to a freshly restarted round would earn.
    Assertions.assertEquals(20_000, round.elapsedAnsweringTime(resumeTime))
    Assertions.assertEquals(333, pointsFor(round, resumeTime))
  }
}
