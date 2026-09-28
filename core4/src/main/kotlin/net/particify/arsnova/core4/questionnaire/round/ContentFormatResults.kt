/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
@JsonSubTypes(
    JsonSubTypes.Type(ChoiceContentRoundStats::class, name = "CHOICE"),
    JsonSubTypes.Type(WordcloudContentRoundStats::class, name = "WORDCLOUD"),
)
interface ContentFormatResults {
  val format: String
}
