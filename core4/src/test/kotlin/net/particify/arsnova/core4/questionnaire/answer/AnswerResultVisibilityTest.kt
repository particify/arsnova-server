/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private const val DURATION_MS = 1500

class AnswerResultVisibilityTest {
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val visibility = AnswerResultVisibility(questionnaireRepository)
  private val contentResults =
      AnswerResultState.entries.map { ContentAnswerResult(UUID.randomUUID(), it, DURATION_MS) }
  private val questionnaireResult = QuestionnaireAnswerResult(QUESTIONNAIRE_ID, 2, 3, 900, 1500)

  private fun givenSolutionPublished(published: Boolean?) {
    val questionnaire = Questionnaire(id = QUESTIONNAIRE_ID, correctOptionsPublished = published)
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID))
        .thenReturn(Optional.of(questionnaire))
  }

  @Test
  fun shouldShowResultsOncePublished() {
    givenSolutionPublished(true)
    Assertions.assertEquals(
        contentResults, visibility.contentResults(QUESTIONNAIRE_ID, contentResults))
    Assertions.assertEquals(
        questionnaireResult, visibility.questionnaireResult(questionnaireResult))
  }

  @Test
  fun shouldReportScoredAnswersAsNeutralUntilPublished() {
    listOf(false, null).forEach { published ->
      givenSolutionPublished(published)
      val shown = visibility.contentResults(QUESTIONNAIRE_ID, contentResults)
      Assertions.assertEquals(
          listOf(
              AnswerResultState.UNANSWERED,
              AnswerResultState.ABSTAINED,
              AnswerResultState.NEUTRAL,
              AnswerResultState.NEUTRAL,
              AnswerResultState.NEUTRAL),
          shown.map { it.result })
      Assertions.assertEquals(contentResults.map { it.contentId }, shown.map { it.contentId })
      Assertions.assertTrue(shown.all { it.durationMs == DURATION_MS })
    }
  }

  @Test
  fun shouldHideCorrectCountAndScoreButNotTheMaximumUntilPublished() {
    givenSolutionPublished(false)
    Assertions.assertEquals(
        QuestionnaireAnswerResult(QUESTIONNAIRE_ID, 0, 3, 0, 1500),
        visibility.questionnaireResult(questionnaireResult))
  }

  @Test
  fun shouldRejectUnknownQuestionnaire() {
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID)).thenReturn(Optional.empty())
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      visibility.questionnaireResult(questionnaireResult)
    }
  }
}
