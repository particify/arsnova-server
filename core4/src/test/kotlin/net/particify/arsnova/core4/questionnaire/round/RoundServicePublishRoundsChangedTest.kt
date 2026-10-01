/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

class RoundServicePublishRoundsChangedTest {
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val roundStatsEventPublisher = Mockito.mock(RoundStatsEventPublisher::class.java)
  private val roundService =
      RoundServiceImpl(
          roundRepository,
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(AnswerRepository::class.java),
          Mockito.mock(ContentRepository::class.java),
          Mockito.mock(QuestionnaireRepository::class.java),
          QuestionnaireEventPublisher(),
          roundStatsEventPublisher)

  @Test
  fun shouldSignalEveryRoundOfTheContent() {
    val questionnaire = Questionnaire(id = UUID.randomUUID())
    val content = Content(id = UUID.randomUUID(), questionnaire = questionnaire)
    val rounds = listOf(1, 2).map { Round(id = UUID.randomUUID(), content = content, roundNo = it) }
    Mockito.`when`(roundRepository.findByContentIdOrderByRoundNo(content.id!!)).thenReturn(rounds)
    roundService.publishRoundsChanged(content)
    rounds.forEach {
      Mockito.verify(roundStatsEventPublisher)
          .publishAnswerChanged(AnswerChangedEvent(it.id!!, content.id!!, questionnaire.id!!))
    }
    Mockito.verifyNoMoreInteractions(roundStatsEventPublisher)
  }
}
