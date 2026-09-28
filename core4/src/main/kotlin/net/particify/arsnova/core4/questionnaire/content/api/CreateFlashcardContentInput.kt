/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class CreateFlashcardContentInput(
    val questionnaireId: UUID,
    val body: String,
    val answer: String,
)
