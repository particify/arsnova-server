/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.common.TextRenderingService.TextRenderingOptions
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/** Checks the options only: extended for bodies and flashcard answers, the default for labels. */
class ContentQueryControllerRenderingTest {
  private val textRenderingService = Mockito.mock(TextRenderingService::class.java)
  private val controller =
      ContentQueryController(
          Mockito.mock(ContentServiceImpl::class.java),
          textRenderingService,
          Mockito.mock(RoundServiceImpl::class.java),
          Mockito.mock(AnswerRepository::class.java),
          Mockito.mock(SolutionVisibility::class.java))

  @Test
  fun shouldRenderTheBodyWithTheExtendedFeatureset() {
    controller.bodyRendered(Content(id = UUID.randomUUID(), body = "Body"))

    Mockito.verify(textRenderingService).renderText("Body", TextRenderingOptions.EXTENDED_MARKDOWN)
  }

  @Test
  fun shouldRenderTheFlashcardAnswerWithTheExtendedFeatureset() {
    controller.answerRendered(FlashcardContentFormatData("Answer"))

    Mockito.verify(textRenderingService)
        .renderText("Answer", TextRenderingOptions.EXTENDED_MARKDOWN)
  }

  @Test
  fun shouldRenderOptionLabelsWithTheDefaultFeatureset() {
    controller.options(ChoiceContentFormatData(listOf("Option"), null, false))

    Mockito.verify(textRenderingService).renderText("Option", TextRenderingOptions())
  }
}
