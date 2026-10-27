/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID

data class TemplateQueryInput(
    val language: String? = null,
    val search: String? = null,
    val tagIds: List<UUID>? = null,
)
