/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
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

  /** The ranges are decided product rules, so they are spelled out rather than read back. */
  @Test
  fun shouldRejectWeightOutsideOneToFive() {
    listOf(0, -1, 6).forEach { assertRejected(weight = it) }
  }

  @Test
  fun shouldRejectDurationOutsideOneToThreeThousandSixHundredSeconds() {
    listOf(0, -1, 3601).forEach { assertRejected(duration = it) }
  }

  @Test
  fun shouldRejectBlankOrOverlongBody() {
    listOf("", " ", "x".repeat(Content.MAX_BODY_LENGTH + 1)).forEach { assertRejected(body = it) }
  }

  /**
   * Accepted settings pass on to loading the questionnaire, which the mocked repository does not
   * find.
   */
  @Test
  fun shouldAcceptTheRangeLimits() {
    listOf(1 to 1, 3600 to 5, null to null).forEach { (duration, weight) ->
      val questionnaireId = UUID.randomUUID()
      Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
        contentService.create(
            questionnaireId,
            "x".repeat(Content.MAX_BODY_LENGTH),
            formatData,
            duration,
            true,
            weight)
      }
      Mockito.verify(questionnaireRepository).findById(questionnaireId)
    }
  }
}
