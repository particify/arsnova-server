/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData

data class UpdateScaleContentInput(
    val id: UUID,
    val body: String,
    val template: ScaleContentFormatData.Template,
    val neutralOption: Boolean,
    val duration: Int?,
    val abstentionsAllowed: Boolean?,
)
