/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class QuestionnairePublishingServiceImplTest {
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val publishingService =
      QuestionnairePublishingServiceImpl(
          contentRepository,
          questionnaireRepository,
          roundRepository,
          QuestionnaireEventPublisher())
  private val questionnaire =
      Questionnaire(id = UUID.randomUUID(), publishingMode = QuestionnairePublishingMode.LIVE)

  init {
    listOf("First", "Second", "Third").forEach {
      val content =
          Content(
              id = UUID.randomUUID(),
              questionnaire = questionnaire,
              body = it,
              formatData = ChoiceContentFormatData(listOf("A", "B"), null, false))
      questionnaire.contents.add(content)
      Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    }
    Mockito.`when`(roundRepository.save(Mockito.any(Round::class.java))).thenAnswer {
      it.arguments[0]
    }
    Mockito.`when`(contentRepository.save(Mockito.any(Content::class.java))).thenAnswer {
      it.arguments[0]
    }
    Mockito.`when`(questionnaireRepository.save(questionnaire)).thenReturn(questionnaire)
  }

  @Test
  fun shouldReleaseUpToTheStartedContent() {
    val (first, second) = questionnaire.contents
    publishingService.startLiveContent(first.id!!)
    Assertions.assertSame(first, questionnaire.publishedUpToContent)
    publishingService.startLiveContent(second.id!!)
    Assertions.assertSame(second, questionnaire.publishedUpToContent)
    Assertions.assertSame(second.activeRound, questionnaire.activeRound)
  }

  @Test
  fun shouldKeepLaterContentsReleasedWhenStartingAnEarlierOne() {
    val (first, second, third) = questionnaire.contents
    publishingService.startLiveContent(third.id!!)
    publishingService.startLiveContent(first.id!!)
    Assertions.assertSame(third, questionnaire.publishedUpToContent)
    Assertions.assertSame(first.activeRound, questionnaire.activeRound)
    publishingService.startLiveContent(second.id!!)
    Assertions.assertSame(third, questionnaire.publishedUpToContent)
  }
}
