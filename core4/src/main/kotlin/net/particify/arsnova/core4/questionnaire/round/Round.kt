/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.questionnaire.content.Content

/**
 * A round keeps two instants of its lifecycle besides its countdown. [answeringStartedAt] is set
 * the first time answering opens and never changes, so a round which was never presented can be
 * told from one which ran and was stopped. [answeringExpiredAt] is set once, when the countdown
 * first ran out; a manual stop sets nothing. Answering can be restarted after the countdown ran
 * out, so that a content can be presented again without losing its answers, but answers given after
 * that instant earn no points: they are still evaluated, since the competition was over by then,
 * not the question.
 */
@Entity
@Table(schema = "questionnaire")
class Round(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var content: Content? = null,
    var roundNo: Int = 1,
    var closed: Boolean = false,
    var answeringEndTime: Instant? = null,
    var answeringPausedAt: Instant? = null,
    var answeringStartedAt: Instant? = null,
    var answeringExpiredAt: Instant? = null,
    @Embedded var auditMetadata: AuditMetadata = AuditMetadata(),
) {
  /** Whether answering has ever been open. */
  val started: Boolean
    get() = answeringStartedAt != null

  /** Opens answering for a round that has never been opened before. */
  fun startAnswering(duration: Int?, now: Instant = Instant.now()) {
    closed = false
    answeringEndTime = duration?.takeIf { it > 0 }?.let { now.plusSeconds(it.toLong()) }
    answeringPausedAt = null
    recordStart(now)
  }

  private fun recordStart(now: Instant) {
    if (answeringStartedAt == null) answeringStartedAt = now
  }

  /**
   * Records the end of a countdown which ran out, once. A stopped round is judged by its pause
   * instant, like [remainingAnsweringTime] does, so a stop with time left records nothing. Every
   * state change calls this, and a state change is the only time a round is persisted, so a round
   * which expired and was never touched again carries no record. It needs none: nobody could have
   * answered it after it expired.
   */
  private fun recordExpiry(now: Instant) {
    val endTime = answeringEndTime ?: return
    if (answeringExpiredAt == null && !endTime.isAfter(answeringPausedAt ?: now)) {
      answeringExpiredAt = endTime
    }
  }

  /** Whether an answer given at [at] earns points, which it does not once the countdown ran out. */
  fun scoresAnswerAt(at: Instant): Boolean = answeringExpiredAt?.isAfter(at) != false

  /**
   * The countdown left when answering was stopped, or null if this round has no countdown or its
   * countdown had already run out by then. A round stopped without a recorded pause instant is
   * treated as paused now, so no time counts as lost while it was stopped.
   */
  private fun remainingAnsweringTime(now: Instant): Duration? {
    val endTime = answeringEndTime ?: return null
    val remaining = Duration.between(answeringPausedAt ?: now, endTime)
    return remaining.takeIf { !it.isNegative && !it.isZero }
  }

  /**
   * Reopens a round which was stopped while its countdown was still running, carrying the remainder
   * over instead of restamping the full duration.
   */
  private fun resumeAnswering(remaining: Duration, now: Instant) {
    closed = false
    answeringEndTime = now.plus(remaining)
    answeringPausedAt = null
  }

  /**
   * Opens answering for [duration]. A round stopped with time still on the clock keeps what was
   * left; one whose countdown had run out starts the full duration over, because reopening a
   * finished content is a new attempt at it rather than a continuation. Resuming is deliberately
   * not reachable on its own: without a duration to fall back on it could only drop the countdown.
   */
  fun openAnswering(duration: Int?, now: Instant = Instant.now()) {
    recordExpiry(now)
    val remaining = remainingAnsweringTime(now)
    if (remaining != null) resumeAnswering(remaining, now) else startAnswering(duration, now)
  }

  /** Stops answering. Idempotent: a repeated stop does not move the recorded pause instant. */
  fun stopAnswering(now: Instant = Instant.now()) {
    recordExpiry(now)
    closed = true
    if (answeringPausedAt == null) {
      answeringPausedAt = now
    }
  }

  /**
   * Opens answering with no countdown, clearing any countdown state. Used when leaving LIVE
   * publishing mode, where rounds are closed until presented.
   */
  fun openWithoutCountdown(now: Instant = Instant.now()) {
    recordExpiry(now)
    closed = false
    answeringEndTime = null
    answeringPausedAt = null
    recordStart(now)
  }

  fun acceptsAnswers(now: Instant = Instant.now()) =
      !closed && answeringEndTime?.isAfter(now) != false

  /**
   * How long answering has been open, or null for a round without a countdown to measure against.
   * Capped to the content's duration so an answer arriving late cannot report more than the full
   * time, and floored at zero so a resumed countdown cannot report a negative one.
   */
  fun elapsedAnsweringTime(now: Instant = Instant.now()): Int? {
    val endTime = answeringEndTime ?: return null
    val durationSeconds = content?.duration?.takeIf { it > 0 } ?: return null
    val total = durationSeconds * MILLIS_PER_SECOND
    val left = Duration.between(now, endTime).toMillis()
    return (total - left).coerceIn(0, total).toInt()
  }

  companion object {
    private const val MILLIS_PER_SECOND = 1000L

    /**
     * The first round of [content]: closed until the content is presented in LIVE publishing mode,
     * open right away in the other modes.
     */
    fun initial(content: Content, live: Boolean, now: Instant = Instant.now()): Round {
      val round = Round(content = content, roundNo = 1, closed = live)
      if (!live) round.openWithoutCountdown(now)
      return round
    }
  }
}
