/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

private val ROUND_ID = UUID.randomUUID()

/** Hidden answers are left out of every count, as they are out of the round statistics. */
class ContentQueryControllerCountsTest {
  private val roundService = Mockito.mock(RoundServiceImpl::class.java)
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val solutionVisibility = Mockito.mock(SolutionVisibility::class.java)
  private val controller =
      ContentQueryController(
          Mockito.mock(ContentServiceImpl::class.java),
          Mockito.mock(TextRenderingService::class.java),
          roundService,
          answerRepository,
          solutionVisibility)
  private val content = Content(id = UUID.randomUUID())

  /** Only the hidden-filtered queries are stubbed, so a count read any other way comes out as 0. */
  @BeforeEach
  fun givenVisibleCounts() {
    Mockito.`when`(roundService.findByContentIdOrderByRoundNo(content.id!!))
        .thenReturn(listOf(Round(id = ROUND_ID, content = content)))
    Mockito.`when`(solutionVisibility.maySeeStatistics(content)).thenReturn(true)
    Mockito.`when`(solutionVisibility.maySeeSolution(content)).thenReturn(true)
    Mockito.`when`(answerRepository.countByRoundIdAndHiddenFalse(ROUND_ID)).thenReturn(3)
    Mockito.`when`(
            answerRepository.countByRoundIdAndHiddenFalseAndResult(
                ROUND_ID, AnswerResultState.CORRECT))
        .thenReturn(2)
    Mockito.`when`(
            answerRepository.countByRoundIdAndHiddenFalseAndResult(
                ROUND_ID, AnswerResultState.ABSTAINED))
        .thenReturn(1)
  }

  @Test
  fun shouldCountOnlyVisibleAnswers() {
    Assertions.assertEquals(
        listOf(3, 1, 2),
        listOf(
            controller.answerCount(content),
            controller.abstentionCount(content),
            controller.correctAnswerCount(content)))
  }
}
