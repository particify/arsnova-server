/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
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
