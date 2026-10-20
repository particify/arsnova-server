/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.AuditorAware

private val ROUND_ID = UUID.randomUUID()

class AnswerServiceImplNonAnswerableTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val answerService =
      AnswerServiceImpl(
          answerRepository,
          roundRepository,
          Mockito.mock(ContentServiceImpl::class.java),
          RoundStatsEventPublisher(),
          Mockito.mock(QuestionnaireRepository::class.java),
          AuditorAware { Optional.of(UUID.randomUUID()) },
          Mockito.mock(RoundServiceImpl::class.java))

  /** The round is open, so only the format can be what refuses the answer. */
  private fun givenOpenRound(formatData: ContentFormatData) {
    val round = Round(id = ROUND_ID, content = Content(formatData = formatData), closed = false)
    Mockito.`when`(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round))
  }

  @Test
  fun shouldRefuseAnswersAndAbstentionsForNonAnswerableFormats() {
    listOf(FlashcardContentFormatData("Back"), SlideContentFormatData()).forEach {
      givenOpenRound(it)
      Assertions.assertThrows(
          RoundClosedException::class.java,
          { answerService.createAnswer(ROUND_ID, ChoiceAnswerFormatData(listOf(0))) },
          it.format)
      Assertions.assertThrows(
          RoundClosedException::class.java, { answerService.createAbstention(ROUND_ID) }, it.format)
    }
    Mockito.verify(answerRepository, Mockito.never()).saveAndFlush(Mockito.any())
  }
}
