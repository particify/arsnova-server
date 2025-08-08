/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import net.particify.arsnova.core4.qna.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, UUID> {
  fun deleteByQnaId(qnaId: UUID): Int
}
