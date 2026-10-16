/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class ContentAnswersFieldControllerTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val controller = ContentAnswersFieldController(answerRepository)

  @Test
  fun shouldResolveAListOfContentsWithOneQuery() {
    val answered = Content(id = UUID.randomUUID())
    val unanswered = Content(id = UUID.randomUUID())
    val alsoAnswered = Content(id = UUID.randomUUID())
    val ids = listOf(answered.id!!, unanswered.id!!, alsoAnswered.id!!)
    Mockito.`when`(answerRepository.findContentIdsWithAnswers(ids))
        .thenReturn(setOf(answered.id!!, alsoAnswered.id!!))

    val result = controller.hasAnswers(listOf(answered, unanswered, alsoAnswered))

    Assertions.assertEquals(
        listOf(true, false, true), listOf(answered, unanswered, alsoAnswered).map { result[it] })
    Mockito.verify(answerRepository, Mockito.times(1)).findContentIdsWithAnswers(ids)
    Mockito.verifyNoMoreInteractions(answerRepository)
  }
}
