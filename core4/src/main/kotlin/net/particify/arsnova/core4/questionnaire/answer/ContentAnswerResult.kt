/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID

data class ContentAnswerResult(
    val contentId: UUID,
    var result: AnswerResultState,
    var durationMs: Int? = null,
)
