/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.exception.RoundsNotSupportedException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val CONTENT_ID = UUID.randomUUID()

class RoundServiceNewRoundTest {
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val roundService =
      RoundServiceImpl(
          roundRepository,
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(AnswerRepository::class.java),
          contentRepository,
          questionnaireRepository,
          QuestionnaireEventPublisher(),
          RoundStatsEventPublisher())

  private val formatWithoutRounds =
      object : ContentFormatData {
        override val format = "WITHOUT_ROUNDS"

        override fun withoutRuntimeState() = this
      }

  private fun liveQuestionnaire(): Questionnaire {
    val questionnaire =
        Questionnaire(
            id = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            publishingMode = QuestionnairePublishingMode.LIVE)
    listOf("First", "Second", "Third").forEach {
      val content =
          Content(
              id = UUID.randomUUID(),
              questionnaire = questionnaire,
              body = it,
              formatData = ChoiceContentFormatData(listOf("A", "B"), null, false),
              activeRound = Round(id = UUID.randomUUID()))
      questionnaire.contents.add(content)
      Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    }
    Mockito.`when`(roundRepository.save(Mockito.any(Round::class.java))).thenAnswer {
      it.arguments[0]
    }
    Mockito.`when`(contentRepository.save(Mockito.any(Content::class.java))).thenAnswer {
      it.arguments[0]
    }
    return questionnaire
  }

  @Test
  fun shouldKeepLaterContentsReleasedWhenStartingANewRoundOfAnEarlierOne() {
    val questionnaire = liveQuestionnaire()
    val (first, second, third) = questionnaire.contents
    questionnaire.publishedUpToContent = third
    roundService.startNewRound(first.id!!)
    Assertions.assertSame(third, questionnaire.publishedUpToContent)
    Assertions.assertSame(first.activeRound, questionnaire.activeRound)
    roundService.startNewRound(second.id!!)
    Assertions.assertSame(third, questionnaire.publishedUpToContent)
  }

  @Test
  fun shouldReleaseUpToTheContentWhenStartingANewRoundBeyondTheRange() {
    val questionnaire = liveQuestionnaire()
    val (first, second) = questionnaire.contents
    questionnaire.publishedUpToContent = first
    roundService.startNewRound(second.id!!)
    Assertions.assertSame(second, questionnaire.publishedUpToContent)
  }

  @Test
  fun shouldRejectNewRoundForFormatWithoutRoundsBeforeChangingAnything() {
    val activeRound = Round(id = UUID.randomUUID())
    val questionnaire =
        Questionnaire(
            id = UUID.randomUUID(),
            publishingMode = QuestionnairePublishingMode.LIVE,
            activeRound = activeRound)
    val content =
        Content(
            id = CONTENT_ID,
            questionnaire = questionnaire,
            formatData = formatWithoutRounds,
            activeRound = activeRound)
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))

    val thrown =
        Assertions.assertThrows(RoundsNotSupportedException::class.java) {
          roundService.startNewRound(CONTENT_ID)
        }

    Assertions.assertEquals("WITHOUT_ROUNDS", thrown.format)
    Assertions.assertFalse(activeRound.closed)
    Mockito.verifyNoInteractions(roundRepository, questionnaireRepository)
    Mockito.verify(contentRepository, Mockito.never()).save(Mockito.any())
  }
}
