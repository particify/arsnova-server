/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()

/** Settings outside their range are rejected before any entity is loaded or touched. */
class ContentSettingsValidationTest {
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val contentService =
      ContentServiceImpl(
          contentRepository,
          questionnaireRepository,
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(RoundServiceImpl::class.java),
          Mockito.mock(AnswerRepository::class.java),
          QuestionnaireEventPublisher())
  private val formatData = ChoiceContentFormatData(listOf("A", "B"), listOf(0), false)

  private fun assertRejected(body: String = "Body", duration: Int? = null, weight: Int? = null) {
    Assertions.assertThrows(InvalidInputException::class.java) {
      contentService.create(QUESTIONNAIRE_ID, body, formatData, duration, true, weight)
    }
    Assertions.assertThrows(InvalidInputException::class.java) {
      contentService.update(CONTENT_ID, body, formatData, duration, true, weight)
    }
    Mockito.verifyNoInteractions(contentRepository, questionnaireRepository)
  }

  @Test
  fun shouldRejectWeightOutsideTheRange() {
    listOf(0, -1, Content.WEIGHT_RANGE.last + 1).forEach { assertRejected(weight = it) }
  }

  @Test
  fun shouldRejectDurationOutsideTheRange() {
    listOf(0, -1, Content.DURATION_RANGE.last + 1).forEach { assertRejected(duration = it) }
  }

  @Test
  fun shouldRejectBlankOrOverlongBody() {
    listOf("", " ", "x".repeat(Content.MAX_BODY_LENGTH + 1)).forEach { assertRejected(body = it) }
  }

  @Test
  fun shouldAcceptTheRangeLimits() {
    val questionnaireId = UUID.randomUUID()
    Assertions.assertThrows(RuntimeException::class.java) {
      contentService.create(
          questionnaireId,
          "x".repeat(Content.MAX_BODY_LENGTH),
          formatData,
          Content.DURATION_RANGE.last,
          true,
          Content.WEIGHT_RANGE.last)
    }
    Mockito.verify(questionnaireRepository).findById(questionnaireId)
  }
}
