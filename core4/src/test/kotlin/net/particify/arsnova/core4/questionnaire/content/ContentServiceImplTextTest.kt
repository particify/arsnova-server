/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val CONTENT_ID = UUID.randomUUID()

// Keyword and publishing operations only apply to questionnaires presented in a room.
private val roomQuestionnaire = Questionnaire(id = UUID.randomUUID(), roomId = UUID.randomUUID())

class ContentServiceImplTextTest {
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val roundService = Mockito.mock(RoundServiceImpl::class.java)
  private val contentService =
      ContentServiceImpl(
          contentRepository,
          Mockito.mock(QuestionnaireRepository::class.java),
          Mockito.mock(ApplicationEventPublisher::class.java),
          roundService,
          Mockito.mock(AnswerRepository::class.java),
          QuestionnaireEventPublisher())

  private fun givenContent(formatData: ContentFormatData): Content {
    val content =
        Content(
            id = CONTENT_ID,
            questionnaire = roomQuestionnaire,
            body = "old",
            formatData = formatData)
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
    Mockito.`when`(contentRepository.save(content)).thenReturn(content)
    return content
  }

  @Test
  fun shouldPublishAndWithdrawAnswersAndNotifyTheRounds() {
    val content = givenContent(TextContentFormatData(false))
    Assertions.assertEquals(
        TextContentFormatData(true),
        contentService.updateTextAnswersPublished(CONTENT_ID, true).formatData)
    Assertions.assertEquals(
        TextContentFormatData(false),
        contentService.updateTextAnswersPublished(CONTENT_ID, false).formatData)
    Mockito.verify(roundService, Mockito.times(2)).publishRoundsChanged(content)
  }

  @Test
  fun shouldRejectPublishingAnswersOfOtherFormat() {
    val current = WordcloudContentFormatData()
    val content = givenContent(current)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.updateTextAnswersPublished(CONTENT_ID, true)
    }
    Assertions.assertSame(current, content.formatData)
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
    Mockito.verifyNoInteractions(roundService)
  }

  @Test
  fun shouldRejectUpdateSwitchingFromOrToText() {
    listOf(
            ChoiceContentFormatData(listOf("A"), null, false) to TextContentFormatData(false),
            TextContentFormatData(false) to ChoiceContentFormatData(listOf("A"), null, false))
        .forEach { (current, incoming) ->
          val content = givenContent(current)
          Assertions.assertThrows(
              ContentFormatDataInvalidException::class.java,
              { contentService.update(CONTENT_ID, "new", incoming, null, null, null) },
              "${incoming.format} over ${current.format}")
          Assertions.assertSame(current, content.formatData)
          Assertions.assertEquals("old", content.body)
        }
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }
}
