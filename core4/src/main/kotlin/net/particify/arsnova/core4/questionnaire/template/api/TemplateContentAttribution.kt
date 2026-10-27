/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID
import net.particify.arsnova.core4.license.License

/**
 * Credits the template a content of a room's questionnaire was copied from, and the templates that
 * template was made from, nearest first.
 */
data class TemplateContentAttribution(
    val attribution: String?,
    val contentId: UUID,
    val license: License?,
    val templateId: UUID,
    val upstreamAttributions: List<TemplateUpstreamAttribution>,
)
