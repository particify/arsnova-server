/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType

private const val MIN_THRESHOLD = -50L
private const val MAX_THRESHOLD = -5L

@Entity
@Table(schema = "qna")
class Qna(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    val roomId: UUID? = null,
    var topic: String? = null,
    var autoPublish: Boolean = true,
    @JdbcType(PostgreSQLEnumJdbcType::class) var state: QnaState = QnaState.STOPPED,
    @field:Min(MIN_THRESHOLD) @field:Max(MAX_THRESHOLD) var threshold: Int? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_post_id")
    var activePost: Post? = null,
    @OneToMany(mappedBy = "qna", fetch = FetchType.EAGER)
    var tags: MutableSet<Tag> = mutableSetOf(),
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
) {
  fun copy(roomId: UUID): Qna {
    return Qna(
        topic = topic,
        autoPublish = autoPublish,
        state = state,
        threshold = threshold,
        roomId = roomId)
  }
}
