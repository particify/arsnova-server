/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val CONTENT_ID = UUID.randomUUID()

class ContentPointsTest {
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val contentService =
      ContentServiceImpl(
          contentRepository,
          Mockito.mock(QuestionnaireRepository::class.java),
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(RoundServiceImpl::class.java),
          Mockito.mock(AnswerRepository::class.java),
          QuestionnaireEventPublisher())

  private fun update(formatData: ContentFormatData, weight: Int?): Content {
    val content =
        Content(
            id = CONTENT_ID,
            formatData = ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            points = Content.BASE_POINTS)
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
    Mockito.`when`(contentRepository.save(content)).thenReturn(content)
    return contentService.update(CONTENT_ID, "body", formatData, null, null, weight)
  }

  @Test
  fun shouldScaleBasePointsWithWeight() {
    Assertions.assertEquals(Content.BASE_POINTS, Content.calculateBasePoints(null))
    Assertions.assertEquals(Content.BASE_POINTS, Content.calculateBasePoints(1))
    Assertions.assertEquals(3 * Content.BASE_POINTS, Content.calculateBasePoints(3))
  }

  @Test
  fun shouldRecomputePointsFromNewWeightOnUpdate() {
    val updated = update(ChoiceContentFormatData(listOf("A", "B"), listOf(0), false), 2)
    Assertions.assertEquals(2 * Content.BASE_POINTS, updated.points)
  }

  @Test
  fun shouldDropPointsWhenUpdateRemovesTheCorrectOptions() {
    val updated = update(ChoiceContentFormatData(listOf("A", "B"), null, false), 2)
    Assertions.assertEquals(0, updated.points)
  }

  @Test
  fun shouldScoreFormatsWithoutPartialCreditAllOrNothing() {
    val allOrNothing =
        object : ContentFormatData {
          override val format = "CHOICE"

          override fun evaluateAnswer(answerFormatData: AnswerFormatData) =
              if ((answerFormatData as ChoiceAnswerFormatData).selectedChoiceIndexes == listOf(0))
                  AnswerResultState.CORRECT
              else AnswerResultState.WRONG
        }
    val right = ChoiceAnswerFormatData(listOf(0))
    val wrong = ChoiceAnswerFormatData(listOf(1))
    Assertions.assertEquals(
        Content.BASE_POINTS, allOrNothing.calculateAchievedPoints(right, Content.BASE_POINTS))
    Assertions.assertEquals(0, allOrNothing.calculateAchievedPoints(wrong, Content.BASE_POINTS))
  }
}
