/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
@JsonSubTypes(
    JsonSubTypes.Type(BinaryAnswerFormatData::class, name = "BINARY"),
    JsonSubTypes.Type(ChoiceAnswerFormatData::class, name = "CHOICE"),
    JsonSubTypes.Type(NumericAnswerFormatData::class, name = "NUMERIC"),
    JsonSubTypes.Type(PrioritizationAnswerFormatData::class, name = "PRIORITIZATION"),
    JsonSubTypes.Type(ScaleAnswerFormatData::class, name = "SCALE"),
    JsonSubTypes.Type(ShortAnswerAnswerFormatData::class, name = "SHORT_ANSWER"),
    JsonSubTypes.Type(SortAnswerFormatData::class, name = "SORT"),
    JsonSubTypes.Type(TextAnswerFormatData::class, name = "TEXT"),
    JsonSubTypes.Type(WordcloudAnswerFormatData::class, name = "WORDCLOUD"),
)
interface AnswerFormatData {
  val format: String
}
