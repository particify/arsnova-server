/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator

@Entity
@Table(schema = "qna")
class Reply(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var post: Post? = null,
    var body: String? = null,
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata()
)
