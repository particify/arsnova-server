/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.common.UuidV7Generator
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType

/**
 * The target is referenced without a foreign key, and a report outlives both its target and its
 * creator. Once decided, [AuditMetadataUuidV7.updatedBy] and [AuditMetadataUuidV7.updatedAt] record
 * by whom and when, since a decision is final.
 */
@Entity
@Table(schema = "violation_report")
class ViolationReport(
    @Id @UuidV7Generator var id: UUID? = null,
    @Version var version: Int? = 0,
    var targetType: String? = null,
    var targetId: UUID? = null,
    @JdbcType(PostgreSQLEnumJdbcType::class) var reason: ViolationReportReason? = null,
    var description: String? = null,
    @JdbcType(PostgreSQLEnumJdbcType::class) var decision: ViolationReportDecision? = null,
    @Embedded val auditMetadata: AuditMetadataUuidV7 = AuditMetadataUuidV7(),
)
