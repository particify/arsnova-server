/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode

data class UpdateQuestionnaireInput(
    val id: UUID,
    val name: String?,
    val published: Boolean?,
    val publishingMode: QuestionnairePublishingMode?,
    val statisticsPublished: Boolean?,
    val correctOptionsPublished: Boolean?,
    val leaderboardEnabled: Boolean?,
)
