/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class CreateBinaryContentInput(
    val questionnaireId: UUID,
    val body: String,
    val correctAnswer: Boolean?,
    val duration: Int?,
    val weight: Int?,
    val abstentionsAllowed: Boolean?,
)
