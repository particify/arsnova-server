/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.round.Round
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType

@Entity
@Table(schema = "questionnaire")
class Questionnaire(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    var roomId: UUID? = null,
    var name: String? = null,
    @JdbcType(PostgreSQLEnumJdbcType::class) var type: QuestionnaireType? = null,
    var published: Boolean = false,
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var publishingMode: QuestionnairePublishingMode? = QuestionnairePublishingMode.ALL,
    var statisticsPublished: Boolean? = null,
    var correctOptionsPublished: Boolean? = null,
    var leaderboardEnabled: Boolean? = null,
    @OneToMany(mappedBy = "questionnaire", cascade = [CascadeType.ALL])
    @OrderColumn(name = "position")
    var contents: MutableList<Content> = mutableListOf(),
    @ManyToOne var publishedUpToContent: Content? = null,
    @ManyToOne var activeRound: Round? = null,
    @Embedded var auditMetadata: AuditMetadata = AuditMetadata(),
) {
  fun isLive(): Boolean = publishingMode == QuestionnairePublishingMode.LIVE
}
