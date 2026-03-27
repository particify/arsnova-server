/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID
import net.particify.arsnova.core4.common.UuidGenerator

@Entity
@Table(schema = "qna")
class Tag(
    @Id @UuidGenerator var id: UUID? = null,
    @ManyToOne var qna: Qna? = null,
    var name: String? = null
) {
  fun copy(qnaId: UUID): Tag {
    return Tag(qna = Qna(id = qnaId), name = name)
  }
}
