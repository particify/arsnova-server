/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID

data class QuestionnaireAnswerResult(
    val questionnaireId: UUID,
    var correctAnswerCount: Int,
    var scorableAnswerCount: Int,
    var achievedScore: Int,
    var maxScore: Int
)
