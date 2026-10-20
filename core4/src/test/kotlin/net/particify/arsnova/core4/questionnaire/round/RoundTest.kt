/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.time.Instant
import net.particify.arsnova.core4.questionnaire.content.Content
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
  fun shouldRecordWhenTheCountdownRanOutWhenRestartedAfterExpiry() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    val expiredAt = NOW.plusSeconds(DURATION_SECONDS)
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(DURATION_SECONDS + 20))
    Assertions.assertEquals(expiredAt, round.answeringExpiredAt)
    Assertions.assertTrue(round.scoresAnswerAt(expiredAt.minusSeconds(1)))
    Assertions.assertFalse(round.scoresAnswerAt(expiredAt))
    Assertions.assertFalse(round.scoresAnswerAt(NOW.plusSeconds(DURATION_SECONDS + 25)))
  }

  @Test
  fun shouldKeepTheFirstExpiryWhenRestartedAgain() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(DURATION_SECONDS + 20))
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(3 * DURATION_SECONDS))
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), round.answeringExpiredAt)
  }

  @Test
  fun shouldRecordNoExpiryWhenStoppedWithTimeLeft() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(10))
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(DURATION_SECONDS + 20))
    Assertions.assertNull(round.answeringExpiredAt)
    Assertions.assertTrue(round.scoresAnswerAt(NOW.plusSeconds(DURATION_SECONDS + 25)))
  }

  @Test
  fun shouldRecordExpiryWhenStoppedAfterTheCountdownRanOut() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(DURATION_SECONDS + 10))
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(DURATION_SECONDS + 20))
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), round.answeringExpiredAt)
  }

  @Test
  fun shouldRecordExpiryWhenOpenedWithoutCountdownAfterItRanOut() {
    val expired = Round()
    expired.startAnswering(DURATION_SECONDS.toInt(), NOW)
    expired.openWithoutCountdown(NOW.plusSeconds(DURATION_SECONDS + 10))
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), expired.answeringExpiredAt)
    val stopped = Round()
    stopped.startAnswering(DURATION_SECONDS.toInt(), NOW)
    stopped.stopAnswering(NOW.plusSeconds(10))
    stopped.openWithoutCountdown(NOW.plusSeconds(DURATION_SECONDS + 10))
    Assertions.assertNull(stopped.answeringExpiredAt)
  }

  @Test
  fun shouldScoreEveryAnswerWithoutExpiry() {
    val round = Round()
    round.startAnswering(null, NOW)
    round.openAnswering(null, NOW.plusSeconds(DURATION_SECONDS * 100))
    Assertions.assertNull(round.answeringExpiredAt)
    Assertions.assertTrue(round.scoresAnswerAt(NOW.plusSeconds(DURATION_SECONDS * 200)))
  }

  @Test
  fun shouldRecordTheStartOnceWhenAnsweringOpens() {
    val round = Round()
    Assertions.assertFalse(round.started)
    Assertions.assertNull(round.answeringStartedAt)
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    Assertions.assertTrue(round.started)
    Assertions.assertEquals(NOW, round.answeringStartedAt)
    round.stopAnswering(NOW.plusSeconds(10))
    Assertions.assertTrue(round.started)
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(15))
    round.openAnswering(DURATION_SECONDS.toInt(), NOW.plusSeconds(3 * DURATION_SECONDS))
    Assertions.assertEquals(NOW, round.answeringStartedAt)
  }

  @Test
  fun shouldRecordTheStartWhenOpenedWithoutCountdown() {
    val round = Round(closed = true)
    round.openWithoutCountdown(NOW)
    Assertions.assertEquals(NOW, round.answeringStartedAt)
  }

  @Test
  fun shouldStartTheInitialRoundOutsideLiveModeOnly() {
    val live = Round.initial(Content(), live = true, now = NOW)
    Assertions.assertTrue(live.closed)
    Assertions.assertFalse(live.started)
    val open = Round.initial(Content(), live = false, now = NOW)
    Assertions.assertFalse(open.closed)
    Assertions.assertEquals(NOW, open.answeringStartedAt)
    Assertions.assertTrue(open.acceptsAnswers(NOW.plusSeconds(DURATION_SECONDS * 100)))
  }

  @Test
  fun shouldRecordExpiryWhenStoppedAfterTheCountdownRanOutWithoutReopening() {
    val round = Round()
    round.startAnswering(DURATION_SECONDS.toInt(), NOW)
    round.stopAnswering(NOW.plusSeconds(DURATION_SECONDS + 10))
    Assertions.assertEquals(NOW.plusSeconds(DURATION_SECONDS), round.answeringExpiredAt)
    val stoppedInTime = Round()
    stoppedInTime.startAnswering(DURATION_SECONDS.toInt(), NOW)
    stoppedInTime.stopAnswering(NOW.plusSeconds(10))
    Assertions.assertNull(stoppedInTime.answeringExpiredAt)
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
