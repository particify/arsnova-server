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
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.common.UuidV7Generator

@Entity
@Table(schema = "qna")
class Reply(
    @Id @UuidV7Generator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var post: Post? = null,
    var body: String? = null,
    @Embedded val auditMetadata: AuditMetadataUuidV7 = AuditMetadataUuidV7()
) {
  fun copy(postId: UUID): Reply {
    return Reply(post = Post(id = postId), body = body)
  }
}
