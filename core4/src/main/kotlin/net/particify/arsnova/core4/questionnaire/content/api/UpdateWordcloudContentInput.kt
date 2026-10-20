/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class UpdateWordcloudContentInput(
    val id: UUID,
    val body: String,
    val duration: Int?,
    val abstentionsAllowed: Boolean?,
    val maxAnswers: Int,
)
