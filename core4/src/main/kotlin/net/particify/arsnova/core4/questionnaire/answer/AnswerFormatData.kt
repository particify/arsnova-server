/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
@JsonSubTypes(
    JsonSubTypes.Type(ChoiceAnswerFormatData::class, name = "CHOICE"),
    JsonSubTypes.Type(TextAnswerFormatData::class, name = "TEXT"),
    JsonSubTypes.Type(WordcloudAnswerFormatData::class, name = "WORDCLOUD"),
)
interface AnswerFormatData {
  val format: String
}
