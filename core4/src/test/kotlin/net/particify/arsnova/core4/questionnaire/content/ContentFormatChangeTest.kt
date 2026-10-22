/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val CONTENT_ID = UUID.randomUUID()

/** A format-specific update must not turn a content of another format into its own. */
class ContentFormatChangeTest {
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val contentService =
      ContentServiceImpl(
          contentRepository,
          Mockito.mock(QuestionnaireRepository::class.java),
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(RoundServiceImpl::class.java),
          Mockito.mock(AnswerRepository::class.java),
          QuestionnaireEventPublisher())

  private fun choice() = ChoiceContentFormatData(listOf("A", "B"), listOf(0), false)

  private fun assertRejected(formatData: ContentFormatData, current: ContentFormatData) {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      formatData.validateChange(current) { false }
    }
  }

  /** Answered contents lock some of their settings, see the format's own tests. */
  @Test
  fun shouldAllowChangesWithinTheSameFormat() {
    choice().validateChange(ChoiceContentFormatData(listOf("A"), null, true)) { false }
    WordcloudContentFormatData(5).validateChange(WordcloudContentFormatData()) { true }
  }

  @Test
  fun shouldRejectChoiceUpdateOfOtherFormat() {
    assertRejected(choice(), WordcloudContentFormatData())
  }

  @Test
  fun shouldRejectWordcloudUpdateOfOtherFormat() {
    assertRejected(WordcloudContentFormatData(), choice())
  }

  @Test
  fun shouldLeaveContentUnchangedWhenUpdateOfOtherFormatIsRejected() {
    val current = choice()
    val content = Content(id = CONTENT_ID, body = "old", formatData = current)
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))

    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(CONTENT_ID, "new", WordcloudContentFormatData(), null, null, null)
    }

    Assertions.assertSame(current, content.formatData)
    Assertions.assertEquals("old", content.body)
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldNotTurnChoiceContentIntoNonAnswerableFormat() {
    listOf(FlashcardContentFormatData("Back"), SlideContentFormatData()).forEach {
      val current = choice()
      val content = Content(id = CONTENT_ID, formatData = current)
      Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
      Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
        contentService.update(CONTENT_ID, "body", it, null, false, null)
      }
      Assertions.assertSame(current, content.formatData)
    }
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }
}
