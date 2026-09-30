/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID

data class UpdateTemplateInput(
    val id: UUID,
    val aiGenerated: Boolean?,
    val attribution: String?,
    val description: String?,
    val language: String?,
    val licenseId: String?,
    val name: String?,
    val published: Boolean?,
    val tagNames: List<String>?,
)
