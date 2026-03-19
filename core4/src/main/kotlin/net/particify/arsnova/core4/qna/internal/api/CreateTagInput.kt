/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.Tag

data class CreateTagInput(val qnaId: UUID, val name: String) {
  fun toTag() = Tag(qna = Qna(id = qnaId), name = name)
}
