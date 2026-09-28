/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.common.UuidV7Generator
import net.particify.arsnova.core4.questionnaire.round.Round
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType
import org.hibernate.type.SqlTypes

@Entity
@Table(schema = "questionnaire")
data class Answer(
    @Id @UuidV7Generator var id: UUID? = null,
    @ManyToOne var round: Round? = null,
    @Version var version: Int? = 0,
    @JdbcType(PostgreSQLEnumJdbcType::class) var result: AnswerResultState? = null,
    @JdbcTypeCode(SqlTypes.JSON) var formatData: AnswerFormatData? = null,
    var points: Int? = null,
    var durationMs: Int? = null,
    var hidden: Boolean = false,
    @Embedded val auditMetadata: AuditMetadataUuidV7 = AuditMetadataUuidV7()
) {
  @Embeddable data class RoundUserId(var roundId: UUID? = null, var userId: UUID? = null)
}
