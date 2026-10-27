/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID
import net.particify.arsnova.core4.license.License

/** Credits a template which the template a content was copied from was itself made from. */
data class TemplateUpstreamAttribution(
    val attribution: String?,
    val license: License?,
    val templateId: UUID,
)
