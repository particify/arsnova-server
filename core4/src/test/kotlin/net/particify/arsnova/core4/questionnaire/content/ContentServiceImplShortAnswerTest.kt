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

class ContentServiceImplShortAnswerTest {
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
    val content = Content(id = CONTENT_ID, formatData = formatData)
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
    Mockito.`when`(contentRepository.save(content)).thenReturn(content)
    return content
  }

  private fun bannedKeywordsOf(content: Content) =
      (content.formatData as ShortAnswerContentFormatData).bannedKeywords

  @Test
  fun shouldBanAndResetKeywordsOfShortAnswerContent() {
    val content = givenContent(ShortAnswerContentFormatData(listOf("Frankfurt am Main")))
    Assertions.assertEquals(
        listOf("Spam"), bannedKeywordsOf(contentService.banShortAnswerKeyword(CONTENT_ID, "Spam")))
    Assertions.assertEquals(
        listOf<String>(),
        bannedKeywordsOf(contentService.resetBannedShortAnswerKeywords(CONTENT_ID)))
    Mockito.verify(roundService, Mockito.times(2)).publishRoundsChanged(content)
  }

  @Test
  fun shouldRejectShortAnswerKeywordOperationsOnOtherFormatWithoutChangingIt() {
    val formatData = WordcloudContentFormatData()
    val content = givenContent(formatData)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.banShortAnswerKeyword(CONTENT_ID, "Spam")
    }
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.resetBannedShortAnswerKeywords(CONTENT_ID)
    }
    Assertions.assertSame(formatData, content.formatData)
    Assertions.assertEquals(listOf<String>(), formatData.bannedKeywords)
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
    Mockito.verifyNoInteractions(roundService)
  }

  @Test
  fun shouldRejectWordcloudKeywordOperationsOnShortAnswerContent() {
    givenContent(ShortAnswerContentFormatData(listOf("Frankfurt am Main")))
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.banWordcloudKeyword(CONTENT_ID, "Spam")
    }
  }
}
