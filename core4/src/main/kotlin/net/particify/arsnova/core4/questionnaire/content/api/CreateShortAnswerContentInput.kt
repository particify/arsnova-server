/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class CreateShortAnswerContentInput(
    val questionnaireId: UUID,
    val body: String,
    val correctTerms: List<String>,
    val duration: Int?,
    val weight: Int?,
    val abstentionsAllowed: Boolean?,
)
