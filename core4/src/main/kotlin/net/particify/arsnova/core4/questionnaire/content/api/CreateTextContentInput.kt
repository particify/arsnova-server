/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class CreateTextContentInput(
    val format: String = "TEXT",
    val questionnaireId: UUID,
    val body: String,
    val duration: Int?,
    val abstentionsAllowed: Boolean?,
)
