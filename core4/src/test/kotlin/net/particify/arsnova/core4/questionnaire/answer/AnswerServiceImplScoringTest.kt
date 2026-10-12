/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.query.ScrollSubrange

private const val HALF_OF_DOUBLE_WEIGHT = 500
private const val LEGACY_POINTS = 300
private val OPTIONS = listOf("A", "B", "C")
private val USER_ID = UUID.randomUUID()

class AnswerServiceImplScoringTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val contentServiceImpl = Mockito.mock(ContentServiceImpl::class.java)
  private val answerService =
      AnswerServiceImpl(
          answerRepository,
          roundRepository,
          contentServiceImpl,
          RoundStatsEventPublisher(),
          Mockito.mock(QuestionnaireRepository::class.java),
          AuditorAware { Optional.of(USER_ID) })
  private val questionnaire = Questionnaire(id = UUID.randomUUID())

  init {
    Mockito.`when`(answerRepository.saveAndFlush(Mockito.any(Answer::class.java))).thenAnswer {
      it.arguments[0]
    }
  }

  private fun content(weight: Int, points: Int? = null): Content {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = questionnaire,
            formatData = ChoiceContentFormatData(OPTIONS, listOf(0, 1), true),
            weight = weight,
            points = points)
    val round = Round(id = UUID.randomUUID(), content = content)
    content.activeRound = round
    Mockito.`when`(roundRepository.findById(round.id!!)).thenReturn(Optional.of(round))
    return content
  }

  private fun answer(content: Content, vararg selectedChoiceIndexes: Int) =
      answerService.createAnswer(
          content.activeRound!!.id!!, ChoiceAnswerFormatData(selectedChoiceIndexes.toList()))

  @Test
  fun shouldStorePartialPointsOfWeightedContentWithWrongResult() {
    val stored = answer(content(weight = 2), 0)
    Assertions.assertEquals(AnswerResultState.WRONG, stored.result)
    Assertions.assertEquals(HALF_OF_DOUBLE_WEIGHT, stored.points)
  }

  @Test
  fun shouldStoreAnswerWhichAchievedNothingWithoutPoints() {
    Assertions.assertNull(answer(content(weight = 1), 0, 2).points)
  }

  @Test
  fun shouldPreferStoredContentPointsOverWeight() {
    Assertions.assertEquals(LEGACY_POINTS, answer(content(2, LEGACY_POINTS), 0, 1).points)
  }

  @Test
  fun shouldSumPartialScoresAndSkipAbstentions() {
    val correct = content(weight = 1, points = Content.BASE_POINTS)
    val partial = content(weight = 2, points = 2 * Content.BASE_POINTS)
    val abstained = content(weight = 1, points = Content.BASE_POINTS)
    givenOwnAnswer(correct, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenOwnAnswer(partial, Answer(result = AnswerResultState.WRONG, formatData = choice(1)))
    givenOwnAnswer(abstained, Answer(result = AnswerResultState.ABSTAINED))
    Mockito.`when`(
            contentServiceImpl.findByQuestionnaireIdOrderByPosition(
                questionnaire.id!!, ScrollPosition.offset()))
        .thenReturn(Window.from(listOf(correct, partial, abstained)) { ScrollPosition.offset() })

    val results =
        answerService.findQuestionnaireAnswerResults(
            questionnaire.id!!, USER_ID, ScrollSubrange.create(null, null, true))

    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(3, results.scorableAnswerCount)
    Assertions.assertEquals(Content.BASE_POINTS + HALF_OF_DOUBLE_WEIGHT, results.achievedScore)
    Assertions.assertEquals(4 * Content.BASE_POINTS, results.maxScore)
  }

  private fun choice(vararg selectedChoiceIndexes: Int) =
      ChoiceAnswerFormatData(selectedChoiceIndexes.toList())

  private fun givenOwnAnswer(content: Content, answer: Answer) {
    Mockito.`when`(
            answerRepository.findByRoundIdAndAuditMetadataCreatedBy(
                content.activeRound!!.id!!, USER_ID))
        .thenReturn(answer)
  }
}
