/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Hides how a participant's answers were scored until the questionnaire publishes its solution, as
 * v3 does for its per-user summary. The results would otherwise give the solution away.
 */
@Component
class AnswerResultVisibility(private val questionnaireRepository: QuestionnaireRepository) {
  private fun isSolutionPublished(questionnaireId: UUID): Boolean {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    return questionnaire.correctOptionsPublished == true
  }

  fun contentResults(
      questionnaireId: UUID,
      results: List<ContentAnswerResult>
  ): List<ContentAnswerResult> {
    if (isSolutionPublished(questionnaireId)) return results
    return results.map { it.copy(result = withoutCorrectness(it.result)) }
  }

  fun questionnaireResult(result: QuestionnaireAnswerResult): QuestionnaireAnswerResult {
    if (isSolutionPublished(result.questionnaireId)) return result
    return result.copy(correctAnswerCount = 0, achievedScore = 0)
  }

  companion object {
    /** The state as shown to a reader who may not learn whether the answer was correct. */
    fun withoutCorrectness(state: AnswerResultState) =
        if (state == AnswerResultState.CORRECT || state == AnswerResultState.WRONG)
            AnswerResultState.NEUTRAL
        else state
  }
}
