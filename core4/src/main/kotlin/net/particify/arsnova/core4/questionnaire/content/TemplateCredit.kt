/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/** A template a content was copied from, with the licence and attribution it was copied under. */
data class TemplateCredit
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @JsonProperty("templateId") val templateId: UUID,
    @JsonProperty("licenseId") val licenseId: String?,
    @JsonProperty("attribution") val attribution: String?,
)
