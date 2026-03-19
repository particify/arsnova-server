/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.CorrectState
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.PostSortOrder

data class PostQueryInput(
    val qnaId: UUID,
    val moderationState: ModerationState? = null,
    val period: Long? = null,
    val favorite: Boolean? = null,
    val correct: CorrectState? = null,
    val replied: Boolean? = null,
    val tagIds: List<UUID>? = null,
    val search: String? = null,
    val sortOrder: PostSortOrder? = null,
)
