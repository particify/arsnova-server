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
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
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
    const val BASE_POINTS = 500

    /** The worth of a fully correct answer to a content of the given weight. */
    fun calculateBasePoints(weight: Int?): Int = BASE_POINTS * (weight ?: 1)
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
