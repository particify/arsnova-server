/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

/**
 * [correctTerms] are the spellings the creator entered. Any of them makes an answer correct, but
 * only the normalized forms are compared, so case, punctuation and spacing are ignored.
 */
data class ShortAnswerContentFormatData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(@JsonProperty("correctTerms") val correctTerms: List<String>) : KeywordBanning {
  override val format = "SHORT_ANSWER"

  /**
   * Excludes answers from the stats. A keyword matches an answer as a whole, and banning one of the
   * correct terms is allowed: scoring does not look at the banned keywords.
   */
  override var bannedKeywords: MutableList<String> = mutableListOf()

  /**
   * The generated equality leaves out [bannedKeywords] as a property of the body. Persistence
   * detects a change of the format data by equality, so a ban would not be saved otherwise.
   */
  override fun equals(other: Any?) =
      other is ShortAnswerContentFormatData &&
          other.correctTerms == correctTerms &&
          other.bannedKeywords == bannedKeywords

  override fun hashCode() = Objects.hash(correctTerms, bannedKeywords)

  override fun hasCorrectAnswer(): Boolean = correctTerms.isNotEmpty()

  /**
   * The terms are not nullable in the schema, so withholding them leaves an empty list. `copy()`
   * drops [bannedKeywords] as well, which is harmless: they are not part of the schema.
   */
  override fun withoutSolution() = copy(correctTerms = listOf())

  /** `copy()` drops [bannedKeywords], which belong to a presentation of the content. */
  override fun withoutRuntimeState(): ContentFormatData = copy()

  /** Banned keywords are set through their own operations, never through an update. */
  override fun mergeWith(current: ContentFormatData): ContentFormatData {
    val merged = copy()
    if (current is ShortAnswerContentFormatData) {
      merged.bannedKeywords = current.bannedKeywords.toMutableList()
    }
    return merged
  }

  fun isCorrect(body: String): Boolean {
    val normalizedBody = normalizeAnswerText(body)
    return correctTerms.any { normalizeAnswerText(it) == normalizedBody }
  }

  override fun validate() {
    if (correctTerms.isEmpty() || correctTerms.size > MAX_TERM_COUNT) {
      throw ContentFormatDataInvalidException(
          "A short answer content must have between 1 and $MAX_TERM_COUNT correct terms")
    }
    // A term without letters or digits would match every answer that has none either.
    if (correctTerms.any { normalizeAnswerText(it).isEmpty() }) {
      throw ContentFormatDataInvalidException("Correct terms must contain letters or digits")
    }
    if (correctTerms.any { it.length > MAX_TERM_LENGTH }) {
      throw ContentFormatDataInvalidException(
          "Correct terms must not exceed $MAX_TERM_LENGTH characters")
    }
    if (correctTerms.distinctBy { normalizeAnswerText(it) }.size != correctTerms.size) {
      throw ContentFormatDataInvalidException("Correct terms must not repeat")
    }
  }

  /**
   * The result and the points of an answer are stored when it is given, while a participant's score
   * is recomputed from it, so a later change of the terms would set the two apart. Only the set of
   * normalized forms is guarded: reordering or respelling the terms cannot change how any answer
   * was scored, and the order is the creator's own, carrying no meaning of its own.
   */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    val currentFormatData =
        current as? ShortAnswerContentFormatData
            ?: throw ContentFormatDataInvalidException("Content is not a short answer content")
    if (currentFormatData.normalizedTerms() != normalizedTerms() && hasAnswers()) {
      throw ContentFormatDataInvalidException(
          "Correct terms must not be changed once the content has answers")
    }
  }

  private fun normalizedTerms(): Set<String> = correctTerms.map { normalizeAnswerText(it) }.toSet()

  /** Answers are compared in normalized form, so one that normalizes to nothing says nothing. */
  override fun validateAnswer(answerFormatData: AnswerFormatData) {
    super.validateAnswer(answerFormatData)
    val body = (answerFormatData as ShortAnswerAnswerFormatData).body
    if (normalizeAnswerText(body).isEmpty()) {
      throw ContentFormatDataInvalidException("Answer must contain letters or digits")
    }
    if (body.length > MAX_TERM_LENGTH) {
      throw ContentFormatDataInvalidException("Answer must not exceed $MAX_TERM_LENGTH characters")
    }
  }

  override fun evaluateAnswer(answerFormatData: AnswerFormatData): AnswerResultState {
    val body = (answerFormatData as? ShortAnswerAnswerFormatData)?.body
    return when {
      body == null || !hasCorrectAnswer() -> AnswerResultState.NEUTRAL
      isCorrect(body) -> AnswerResultState.CORRECT
      else -> AnswerResultState.WRONG
    }
  }

  companion object {
    const val MAX_TERM_COUNT = 12
    const val MAX_TERM_LENGTH = 80
  }
}
