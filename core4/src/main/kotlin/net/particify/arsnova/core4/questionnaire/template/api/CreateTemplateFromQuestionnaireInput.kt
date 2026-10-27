/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID

data class CreateTemplateFromQuestionnaireInput(
    val aiGenerated: Boolean = false,
    val attribution: String?,
    val description: String?,
    val language: String,
    val licenseId: String?,
    val name: String,
    val published: Boolean = false,
    val questionnaireId: UUID,
    val tagNames: List<String>?,
)
