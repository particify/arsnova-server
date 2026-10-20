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

class ContentServiceImplWordcloudTest {
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
      (content.formatData as WordcloudContentFormatData).bannedKeywords

  @Test
  fun shouldKeepBannedKeywordsWhenContentIsUpdated() {
    val current = WordcloudContentFormatData()
    current.addBannedKeyword("spam")
    givenContent(current)
    val updated =
        contentService.update(CONTENT_ID, "body", WordcloudContentFormatData(5), null, null, null)
    Assertions.assertEquals(listOf("spam"), bannedKeywordsOf(updated))
  }

  @Test
  fun shouldBanAndResetKeywordsOfWordcloudContent() {
    givenContent(WordcloudContentFormatData())
    Assertions.assertEquals(
        listOf("spam"), bannedKeywordsOf(contentService.banWordcloudKeyword(CONTENT_ID, "spam")))
    Assertions.assertEquals(
        listOf<String>(), bannedKeywordsOf(contentService.resetBannedWordcloudKeywords(CONTENT_ID)))
  }

  @Test
  fun shouldSignalTheContentsRoundsAfterBanningAndResetting() {
    val content = givenContent(WordcloudContentFormatData())
    contentService.banWordcloudKeyword(CONTENT_ID, "spam")
    Mockito.verify(roundService).publishRoundsChanged(content)
    contentService.resetBannedWordcloudKeywords(CONTENT_ID)
    Mockito.verify(roundService, Mockito.times(2)).publishRoundsChanged(content)
  }

  @Test
  fun shouldRejectKeywordOperationsOnOtherFormatWithoutChangingIt() {
    val formatData = ChoiceContentFormatData(listOf("A"), null, false)
    val content = givenContent(formatData)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.banWordcloudKeyword(CONTENT_ID, "spam")
    }
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.resetBannedWordcloudKeywords(CONTENT_ID)
    }
    Assertions.assertSame(formatData, content.formatData)
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
    Mockito.verifyNoInteractions(roundService)
  }
}
