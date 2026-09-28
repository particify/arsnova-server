/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val NOW = Instant.parse("2026-01-01T00:00:00Z")
private const val DURATION_SECONDS = 30L

class RoundTest {
  @Test
  fun shouldNotAcceptAnswersWhenNeverStarted() {
    val round = Round(closed = true)
    Assertions.assertFalse(round.acceptsAnswers(NOW))
  }

  @Test
  fun shouldAcceptAnswersWhenRunningWithDuration() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), round.answeringEndTime)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertFalse(round.closed)
    Assertions.assertTrue(round.acceptsAnswers(NOW))
  }

  @Test
  fun shouldNotAcceptAnswersWhenRunningRoundExpiredButNotStopped() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    val afterExpiry = NOW.plusSeconds(DURATION_SECONDS + 1)
    Assertions.assertFalse(round.closed)
    Assertions.assertFalse(round.acceptsAnswers(afterExpiry))
  }

  @Test
  fun shouldAcceptAnswersWhenRunningWithoutDuration() {
    val round = Round()
    round.startAnswering(null, NOW)
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertTrue(round.acceptsAnswers(NOW.plusSeconds(DURATION_SECONDS * 100)))
  }

  @Test
  fun shouldNotAcceptAnswersWhenStopped() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(5))
    Assertions.assertTrue(round.closed)
    Assertions.assertFalse(round.acceptsAnswers(NOW.plusSeconds(5)))
  }

  @Test
  fun shouldRecordPausedAtOnlyOnFirstStop() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    val firstStop = NOW.plusSeconds(10)
    round.stopAnswering(firstStop)
    Assertions.assertEquals(firstStop, round.answeringPausedAt)
    round.stopAnswering(NOW.plusSeconds(20))
    Assertions.assertEquals(firstStop, round.answeringPausedAt)
  }

  @Test
  fun shouldResumeWithRemainingDurationWhenStoppedWithTimeLeft() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(10)) // 20s remaining
    val reopenTime = NOW.plusSeconds(15)
    round.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    Assertions.assertFalse(round.closed)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertEquals(reopenTime.plusSeconds(20), round.answeringEndTime)
    Assertions.assertTrue(
        round.answeringEndTime!!.isBefore(reopenTime.plusSeconds(DURATION_SECONDS)))
    Assertions.assertTrue(round.acceptsAnswers(reopenTime))
  }

  @Test
  fun shouldStartAFreshCountdownWhenRestartedAfterExpiry() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    val reopenTime = NOW.plusSeconds(DURATION_SECONDS + 20) // expired, never explicitly stopped
    Assertions.assertFalse(round.acceptsAnswers(reopenTime))
    round.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    Assertions.assertFalse(round.closed)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertEquals(reopenTime.plusSeconds(DURATION_SECONDS), round.answeringEndTime)
    Assertions.assertTrue(round.acceptsAnswers(reopenTime))
    Assertions.assertFalse(round.acceptsAnswers(reopenTime.plusSeconds(DURATION_SECONDS + 1)))
  }

  @Test
  fun shouldStartAFreshCountdownWhenRestartedAfterBeingStoppedPastExpiry() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(DURATION_SECONDS + 10)) // already expired when stopped
    val reopenTime = NOW.plusSeconds(DURATION_SECONDS + 20)
    round.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    Assertions.assertFalse(round.closed)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertEquals(reopenTime.plusSeconds(DURATION_SECONDS), round.answeringEndTime)
    Assertions.assertFalse(round.acceptsAnswers(reopenTime.plusSeconds(DURATION_SECONDS + 1)))
  }

  @Test
  fun shouldStayUntimedWhenReopeningUntimedRound() {
    val round = Round()
    round.startAnswering(null, NOW)
    round.stopAnswering(NOW.plusSeconds(5))
    round.openAnswering(null, NOW.plusSeconds(10))
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertFalse(round.closed)
  }

  @Test
  fun shouldResumeFromFirstStopWhenStoppedTwice() {
    val singleStop = Round()
    singleStop.startAnswering(DURATION_SECONDS.toInt(), NOW)
    singleStop.stopAnswering(NOW.plusSeconds(10))

    val doubleStop = Round()
    doubleStop.startAnswering(DURATION_SECONDS.toInt(), NOW)
    doubleStop.stopAnswering(NOW.plusSeconds(10))
    doubleStop.stopAnswering(NOW.plusSeconds(20))

    val reopenTime = NOW.plusSeconds(25)
    singleStop.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    doubleStop.openAnswering(DURATION_SECONDS.toInt(), reopenTime)

    Assertions.assertEquals(singleStop.answeringEndTime, doubleStop.answeringEndTime)
  }

  @Test
  fun shouldTreatReopenedRoundWithoutPausedAtAsPausedNow() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    val originalEndTime = round.answeringEndTime
    round.closed = true // simulate a stop that, defensively, never recorded a pause instant
    val reopenTime = NOW.plusSeconds(5)
    round.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    // With no recorded pause instant, "now" is treated as the pause instant too, so no time is
    // assumed lost during the pause and the original deadline carries forward unchanged.
    Assertions.assertEquals(originalEndTime, round.answeringEndTime)
  }

  @Test
  fun shouldBeOpenWithoutCountdownAfterOpeningWithoutCountdown() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(10))
    round.openWithoutCountdown()
    Assertions.assertFalse(round.closed)
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertNull(round.answeringPausedAt)
    Assertions.assertTrue(round.acceptsAnswers(NOW.plusSeconds(DURATION_SECONDS + 10)))
  }

  @Test
  fun shouldStartFreshWhenOpeningNeverStartedRound() {
    val round = Round(closed = true)
    round.openAnswering(DURATION_SECONDS.toInt(), NOW)
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), round.answeringEndTime)
    Assertions.assertFalse(round.closed)
  }

  @Test
  fun shouldResumeWhenOpeningPreviouslyStoppedRound() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(10)) // 20s remaining
    val reopenTime = NOW.plusSeconds(15)
    round.openAnswering(DURATION_SECONDS.toInt(), reopenTime)
    Assertions.assertEquals(reopenTime.plusSeconds(20), round.answeringEndTime)
  }
}
