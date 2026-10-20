/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType

data class CreateQuestionnaireInput(
    val roomId: UUID,
    val name: String,
    val type: QuestionnaireType
)
