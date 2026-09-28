/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID

data class UpdateNumericContentInput(
    val id: UUID,
    val body: String,
    val minNumber: Double,
    val maxNumber: Double,
    val correctNumber: Double?,
    val tolerance: Double?,
    val duration: Int?,
    val weight: Int?,
    val abstentionsAllowed: Boolean?,
)
