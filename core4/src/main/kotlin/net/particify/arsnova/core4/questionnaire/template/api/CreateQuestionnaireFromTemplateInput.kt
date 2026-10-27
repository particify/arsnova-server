/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID

data class CreateQuestionnaireFromTemplateInput(
    val name: String?,
    val roomId: UUID,
    val templateId: UUID,
)
