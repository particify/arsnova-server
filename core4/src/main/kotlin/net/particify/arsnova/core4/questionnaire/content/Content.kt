/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Max
import java.util.UUID
import kotlin.math.roundToInt
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.round.Round
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(schema = "questionnaire")
data class Content(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var questionnaire: Questionnaire? = null,
    var body: String? = null,
    @JdbcTypeCode(SqlTypes.JSON) var formatData: ContentFormatData? = null,
    @field:Max(MAX_DURATION) var duration: Int? = null,
    var weight: Int? = 1,
    var abstentionsAllowed: Boolean = true,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_round_id")
    var activeRound: Round? = null,
    var points: Int? = null,
    @Column(name = "position", insertable = false, updatable = false) var position: Int? = null,
    @Embedded var auditMetadata: AuditMetadata = AuditMetadata(),
) {
  companion object {
    const val MAX_DURATION = 3600L
    const val MAX_BODY_LENGTH = 2000
    const val BASE_POINTS = 500
    private const val MILLIS_PER_SECOND = 1000L
    // Multiplying by 2 is what makes taking less than half of the duration for answering result in
    // additional points while taking more time results in subtracted points in comparison to the
    // base points given for correct answers.
    private const val COMPETITIVE_FACTOR = 2.0
    val DURATION_RANGE = 1..MAX_DURATION.toInt()
    val WEIGHT_RANGE = 1..5

    /** The worth of a correct answer before it is weighted by answering speed. */
    fun calculateBasePoints(weight: Int?): Int = BASE_POINTS * (weight ?: 1)

    /** A null duration means no countdown and a null weight the default one. */
    fun validateSettings(body: String, duration: Int?, weight: Int?) {
      if (body.isBlank() || body.length > MAX_BODY_LENGTH) {
        throw InvalidInputException(
            "The body must not be blank or longer than $MAX_BODY_LENGTH characters")
      }
      if (duration != null && duration !in DURATION_RANGE) {
        throw InvalidInputException("The duration must be within $DURATION_RANGE seconds")
      }
      if (weight != null && weight !in WEIGHT_RANGE) {
        throw InvalidInputException("The weight must be within $WEIGHT_RANGE")
      }
    }
  }

  /**
   * Weights [achievedPoints] by the time left when the answer was given: answering instantly is
   * worth twice the points, answering after half of the duration the plain value and answering at
   * the buzzer nothing. Contents which are answered without a countdown keep their fixed value, so
   * [elapsedMs] is null for those.
   */
  fun calculateCompetitivePoints(achievedPoints: Int, elapsedMs: Int?): Int {
    val durationSeconds = duration?.takeIf { it > 0 } ?: return achievedPoints
    if (elapsedMs == null) return achievedPoints
    val timeLeftMs = durationSeconds * MILLIS_PER_SECOND - elapsedMs
    val weightedPoints =
        COMPETITIVE_FACTOR * timeLeftMs / MILLIS_PER_SECOND / durationSeconds * achievedPoints
    return weightedPoints.roundToInt()
  }

  fun copy(): Content {
    return Content(
        questionnaire = this.questionnaire,
        body = this.body,
        formatData = this.formatData,
        duration = this.duration,
        weight = this.weight,
        abstentionsAllowed = this.abstentionsAllowed,
        points = this.points,
    )
  }
}
